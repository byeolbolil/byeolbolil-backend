package xyz.ncookie.stargazer.domain.member.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.domain.AuthDomainService;
import xyz.ncookie.stargazer.domain.member.domain.EmailVerificationDomainService;
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
			Member findMember = authDomainService.findByEmail(request.email());
			throw new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED, findMember.getAuthProvider().getDesc());
		}

		String nickname = memberDomainService.validateNewNickname(request.nickname(), null);
		// 다른 검사를 모두 지난 뒤에 코드를 쓴다. 닉네임이 겹쳐서 실패했는데 코드만 써 버리면 다시 받아야 한다
		emailVerificationDomainService.verify(request.email(), request.emailCode());
		String encodedPassword = authDomainService.encodePassword(request.password());
		Member createdMember;
		try {
			createdMember = memberRepository.saveAndFlush(Member.local(request.email(), encodedPassword, nickname));
		} catch (DataIntegrityViolationException e) {
			// 검사를 지난 뒤 같은 순간에 다른 가입이 같은 이메일이나 닉네임을 가져간 경우
			RuntimeException conflict = memberDomainService.nicknameConflictOr(e);
			throw conflict instanceof MemberException ? conflict : new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED);
		}

		return issueTokens(createdMember.getId());
	}

	@Transactional(readOnly = true)
	public TokenDto login(LoginRequest request) {

		Member member = authDomainService.authenticate(request.email(), request.password());

		return issueTokens(member.getId());
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
