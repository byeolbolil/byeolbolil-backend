package xyz.ncookie.stargazer.global.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class OAuth2LoginFailureHandlerTest {

	@Test
	@DisplayName("소셜 로그인에 실패하면 설정된 웹 주소의 로그인 화면으로 보내고, 예외 메시지는 주소에 싣지 않는다")
	void redirectsToConfiguredClient() throws Exception {
		OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler("https://www.example.com");
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationFailure(
			new MockHttpServletRequest(), response, new OAuth2AuthenticationException("invalid_token_response"));

		assertThat(response.getRedirectedUrl()).isEqualTo("https://www.example.com/login?error=oauth");
	}
}
