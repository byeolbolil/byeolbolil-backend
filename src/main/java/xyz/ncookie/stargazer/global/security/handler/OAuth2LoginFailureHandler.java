package xyz.ncookie.stargazer.global.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.security.oauth.WithdrawalAwareAuthorizationRequestRepository;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private final String webClientBaseUrl;

	public OAuth2LoginFailureHandler(@Value("${client.redirect-base-url}") String webClientBaseUrl) {
		this.webClientBaseUrl = webClientBaseUrl;
	}

	/**
	 * 로그인 화면으로 돌려보낸다. 예외 메시지에는 제공자 쪽 응답이 섞여 있을 수 있어 주소에 싣지 않고 로그에만 남긴다.
	 * 탈퇴하려다 실패했으면(제공자 로그인을 취소했거나 다른 계정으로 로그인한 경우) 아직 로그인된 상태이므로 첫 화면으로 보낸다.
	 */
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException, ServletException {

		if (WithdrawalAwareAuthorizationRequestRepository.withdrawalTicket(request) != null) {
			log.warn("소셜 회원 탈퇴 실패: {}", exception.getMessage());
			response.sendRedirect(webClientBaseUrl + "/?withdrawal=failed");
			return;
		}

		log.error("OAuth2 로그인 실패: {}", exception.getMessage());

		response.sendRedirect(webClientBaseUrl + "/login?error=oauth");
	}
}
