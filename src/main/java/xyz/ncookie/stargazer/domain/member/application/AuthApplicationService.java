package xyz.ncookie.stargazer.domain.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.domain.AuthDomainService;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
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

	@Transactional
	public TokenDto register(@Valid RegisterRequest request) {

		if (memberDomainService.isEmailDuplicated(request.email())) {
			Member findMember = authDomainService.findByEmail(request.email());
			throw new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED, findMember.getAuthProvider().getDesc());
		}

		String encodedPassword = authDomainService.encodePassword(request.password());
		Member createdMember = memberRepository.save(
			Member.local(
				request.email(),
				encodedPassword,
				request.nickname()
			)
		);

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
