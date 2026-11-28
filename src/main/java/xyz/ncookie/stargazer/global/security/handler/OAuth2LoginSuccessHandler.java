package xyz.ncookie.stargazer.global.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;
import xyz.ncookie.stargazer.global.security.util.CookieUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	private final JwtTokenProvider jwtTokenProvider;
	private final CookieUtil cookieUtil;

	@Value("${client.redirect-base-url}")
	private String webClientBaseUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException {

		log.info("OAuth2 로그인 성공!");

		MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

		if (principal.isWithdrawn()) {
			finishWithdrawal(request, response);
			return;
		}

		Long memberId = principal.getMemberId();

		String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

		refreshTokenRedisRepository.save(refreshToken, memberId, JwtTokenProvider.REFRESH_EXPIRE_MS);

		ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(refreshToken);
		response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

		// 방금 가입한 사람이면 프론트가 첫 닉네임 화면을 띄우도록 알린다
		String callback = webClientBaseUrl + "/oauth/callback" + (principal.isNewMember() ? "?welcome=1" : "");
		response.sendRedirect(callback);
	}

	/**
	 * 탈퇴한 브라우저의 리프레시 토큰을 지우고 프론트 첫 화면으로 돌려보낸다. 다른 기기에 남은 토큰은
	 * 재발급할 때 회원이 없어 거절된다.
	 */
	private void finishWithdrawal(HttpServletRequest request, HttpServletResponse response) throws IOException {

		log.info("소셜 회원 탈퇴 완료");
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (CookieUtil.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
					refreshTokenRedisRepository.delete(cookie.getValue());
				}
			}
		}
		response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshTokenCookie().toString());
		response.sendRedirect(webClientBaseUrl + "/?withdrawn=1");
	}
}
