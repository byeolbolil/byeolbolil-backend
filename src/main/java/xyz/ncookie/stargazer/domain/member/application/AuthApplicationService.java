package xyz.ncookie.stargazer.domain.member.application;

import java.time.Duration;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.domain.AuthDomainService;
import xyz.ncookie.stargazer.domain.member.domain.EmailVerificationDomainService;
import xyz.ncookie.stargazer.domain.member.domain.EmailVerificationDomainService.Purpose;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.repository.LoginAttemptRedisRepository;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.exception.EmailCodeErrorCode;
import xyz.ncookie.stargazer.global.mail.ResendMailClient;
import xyz.ncookie.stargazer.domain.member.dto.TokenDto;
import xyz.ncookie.stargazer.domain.member.dto.request.LoginRequest;
import xyz.ncookie.stargazer.domain.member.dto.request.RegisterRequest;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.AuthErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.AuthException;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthApplicationService {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;
	private final MemberRepository memberRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthDomainService authDomainService;
	private final MemberDomainService memberDomainService;
	private final EmailVerificationDomainService emailVerificationDomainService;
	private final ResendMailClient resendMailClient;
	private final LoginAttemptRedisRepository loginAttemptRedisRepository;

	// 서비스 정책이다. 근거 자료가 있는 값이 아니다(docs/decisions.md 2026-11-28 "이메일 로그인은 다섯 번 틀리면 15분 막는다")
	static final int MAX_LOGIN_FAILURES = 5;
	static final Duration LOGIN_BLOCK = Duration.ofMinutes(15);

	@Transactional(readOnly = true)
	public TokenDto reissueAccessToken(String refreshToken) {

		if (refreshToken == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		Long memberId = refreshTokenRedisRepository.find(refreshToken);
		if (memberId == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		refreshTokenRedisRepository.delete(refreshToken);

		// 탈퇴한 회원의 다른 기기에 남은 토큰. 토큰은 회원별로 찾을 수 없어 탈퇴할 때 지우지 못하고 여기서 막는다
		if (!memberRepository.existsById(memberId)) {
			log.debug("토큰 재발급 실패! 탈퇴한 회원입니다.");
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		TokenDto tokens = issueTokens(memberId);

		log.debug("토큰 재발급 성공!");
		return tokens;
	}

	@Transactional
	public void logout(String refreshToken) {

		if (refreshToken == null) {
			return;
		}

		refreshTokenRedisRepository.delete(refreshToken);
		log.debug("로그아웃 성공!");
	}

	/** 가입할 이메일로 인증 코드를 보낸다. 이미 가입된 이메일이면 보내지 않고 409다 */
	public void sendEmailCode(String email) {

		if (memberDomainService.isEmailDuplicated(email.strip())) {
			throw new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED);
		}

		String code = emailVerificationDomainService.issue(email);
		try {
			resendMailClient.send(email.strip(), "[별볼일] 가입 인증 코드 " + code,
				"별볼일 가입 인증 코드는 " + code + " 입니다.\n\n10분 안에 가입 창에 입력해 주세요. "
					+ "가입하려던 적이 없다면 이 메일은 지워도 됩니다.");
		} catch (ResendMailClient.MailSendException e) {
			log.warn("가입 인증 메일 발송 실패", e);
			emailVerificationDomainService.cancel(email);
			throw new MemberException(EmailCodeErrorCode.MAIL_UNAVAILABLE);
		}
	}

	@Transactional
	public TokenDto register(@Valid RegisterRequest request) {

		if (memberDomainService.isEmailDuplicated(request.email())) {
			throw new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED);
		}

		String nickname = memberDomainService.validateNewNickname(request.nickname(), null);
		// 다른 검사를 모두 지난 뒤에 코드를 쓴다. 닉네임이 겹쳐서 실패했는데 코드만 써 버리면 다시 받아야 한다
		emailVerificationDomainService.verify(request.email(), request.emailCode());
		String encodedPassword = authDomainService.encodePassword(request.password());
		Member createdMember;
		try {
			createdMember = memberRepository.saveAndFlush(Member.local(request.email(), encodedPassword, nickname));
		} catch (DataIntegrityViolationException e) {
			// 검사를 지난 뒤 같은 순간에 다른 가입이 같은 닉네임을 가져간 경우
			throw memberDomainService.nicknameConflictOr(e);
		}

		return issueTokens(createdMember.getId());
	}

	/**
	 * 같은 이메일로 다섯 번 틀리면 15분 동안 맞는 비밀번호도 받지 않는다. 성공하면 센 횟수를 지운다.
	 */
	@Transactional(readOnly = true)
	public TokenDto login(LoginRequest request) {

		String key = EmailVerificationDomainService.keyOf(request.email());
		if (loginAttemptRedisRepository.failures(key) >= MAX_LOGIN_FAILURES) {
			throw new MemberException(MemberErrorCode.TOO_MANY_LOGIN_FAILURES);
		}

		Member member;
		try {
			member = authDomainService.authenticate(request.email(), request.password());
		} catch (MemberException e) {
			if (e.getErrorCode() == MemberErrorCode.LOGIN_FAILED
				&& loginAttemptRedisRepository.addFailure(key, LOGIN_BLOCK) >= MAX_LOGIN_FAILURES) {
				loginAttemptRedisRepository.hold(key, LOGIN_BLOCK);
			}
			throw e;
		}

		loginAttemptRedisRepository.clear(key);
		return issueTokens(member.getId());
	}

	/**
	 * 비밀번호 재설정 코드를 보낸다. 가입 여부를 응답으로 드러내지 않도록 이메일 회원이 없어도 같은 200이다.
	 * 소셜 로그인으로만 쓰는 이메일이면 코드 대신 어느 서비스로 가입했는지 알려 주는 메일을 보낸다.
	 * 발송 제한(429)은 가입 여부와 상관없이 같게 걸리도록 코드부터 만든다.
	 */
	@Transactional(readOnly = true)
	public void sendPasswordResetCode(String rawEmail) {

		String email = rawEmail.strip();
		String code = emailVerificationDomainService.issue(Purpose.PASSWORD_RESET, email);

		String subject;
		String body;
		if (memberRepository.findByEmailAndAuthProvider(email, AuthProvider.LOCAL).isPresent()) {
			subject = "[별볼일] 비밀번호 재설정 코드 " + code;
			body = "별볼일 비밀번호 재설정 코드는 " + code + " 입니다.\n\n10분 안에 비밀번호 찾기 창에 입력해 주세요. "
				+ "요청한 적이 없다면 이 메일은 지워도 됩니다. 비밀번호는 바뀌지 않습니다.";
		} else {
			List<String> providers = memberRepository.findAllByEmail(email).stream()
				.map(member -> member.getAuthProvider().getDesc())
				.distinct()
				.toList();
			if (providers.isEmpty()) {
				return;
			}
			subject = "[별볼일] 로그인 안내";
			body = "이 이메일로 이메일 가입한 별볼일 계정은 없습니다. " + String.join(", ", providers)
				+ " 로그인으로 가입한 계정이 있으니 그 버튼으로 로그인해 주세요.\n\n"
				+ "요청한 적이 없다면 이 메일은 지워도 됩니다.";
		}

		try {
			resendMailClient.send(email, subject, body);
		} catch (ResendMailClient.MailSendException e) {
			log.warn("비밀번호 재설정 메일 발송 실패", e);
			emailVerificationDomainService.cancel(Purpose.PASSWORD_RESET, email);
			throw new MemberException(EmailCodeErrorCode.MAIL_UNAVAILABLE);
		}
	}

	/**
	 * 코드를 확인하고 새 비밀번호를 저장한다. 모든 기기의 로그인을 끊고, 막혀 있던 로그인도 풀어 준다.
	 */
	@Transactional
	public void resetPassword(String rawEmail, String code, String newPassword) {

		String email = rawEmail.strip();
		emailVerificationDomainService.verify(Purpose.PASSWORD_RESET, email, code);
		// 코드는 받을 수 있었지만 이메일 회원이 없는 경우. 코드가 틀렸을 때와 같은 답을 한다
		Member member = memberRepository.findByEmailAndAuthProvider(email, AuthProvider.LOCAL)
			.orElseThrow(() -> new MemberException(EmailCodeErrorCode.CODE_EXPIRED));

		member.changePassword(authDomainService.encodePassword(newPassword));
		refreshTokenRedisRepository.deleteAllOf(member.getId());
		loginAttemptRedisRepository.clear(EmailVerificationDomainService.keyOf(email));
		log.info("회원 {} 비밀번호 재설정", member.getId());
	}

	/**
	 * 로그인한 이메일 회원이 비밀번호를 바꾼다. 다른 기기의 로그인을 끊고 이 기기에는 새 토큰을 준다.
	 */
	@Transactional
	public TokenDto changePassword(Long memberId, String currentPassword, String newPassword) {

		Member member = memberDomainService.findById(memberId);
		if (member.getAuthProvider() != AuthProvider.LOCAL) {
			throw new MemberException(MemberErrorCode.NO_PASSWORD);
		}
		authDomainService.verifyPassword(member, currentPassword);

		member.changePassword(authDomainService.encodePassword(newPassword));
		refreshTokenRedisRepository.deleteAllOf(memberId);
		log.info("회원 {} 비밀번호 변경", memberId);
		return issueTokens(memberId);
	}

	/**
	 * 재발급은 Redis에 있는 리프레시 토큰만 받아 주므로, 토큰을 내줄 때마다 저장해야 한다.
	 */
	private TokenDto issueTokens(Long memberId) {

		String accessToken = jwtTokenProvider.createAccessToken(memberId);
		String refreshToken = jwtTokenProvider.createRefreshToken(memberId);
		refreshTokenRedisRepository.save(refreshToken, memberId, JwtTokenProvider.REFRESH_EXPIRE_MS);

		return new TokenDto(accessToken, refreshToken);
	}
}
