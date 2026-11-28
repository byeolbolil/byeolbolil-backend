package xyz.ncookie.stargazer.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import xyz.ncookie.stargazer.global.security.handler.OAuth2LoginFailureHandler;

/**
 * 로그인 주소에 붙은 탈퇴 표가 세션을 거쳐 제공자 콜백까지 따라오는지 본다.
 */
class WithdrawalAuthorizationTest {

	private final WithdrawalAuthorizationRequestResolver resolver = new WithdrawalAuthorizationRequestResolver(
		new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("kakao")
			.clientId("test-kakao-id")
			.clientSecret("test-kakao-secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri("https://kauth.kakao.com/oauth/authorize")
			.tokenUri("https://kauth.kakao.com/oauth/token")
			.build()));

	private final WithdrawalAwareAuthorizationRequestRepository repository =
		new WithdrawalAwareAuthorizationRequestRepository();

	private final MockHttpSession session = new MockHttpSession();

	@Test
	@DisplayName("탈퇴 표를 붙여 시작한 로그인은 콜백에서 그 표를 꺼낼 수 있다")
	void ticketReachesCallback() {
		MockHttpServletRequest callback = callbackOf(start("ticket-1"));

		assertThat(WithdrawalAwareAuthorizationRequestRepository.withdrawalTicket(callback)).isEqualTo("ticket-1");
	}

	@Test
	@DisplayName("표 없이 시작한 로그인은 콜백에서도 표가 없다")
	void plainLogin() {
		MockHttpServletRequest callback = callbackOf(start(null));

		assertThat(WithdrawalAwareAuthorizationRequestRepository.withdrawalTicket(callback)).isNull();
	}

	@Test
	@DisplayName("탈퇴하려다 실패하면 로그인 화면이 아니라 첫 화면으로 돌려보낸다. 아직 로그인된 상태이기 때문이다")
	void failedWithdrawalReturnsHome() throws Exception {
		MockHttpServletRequest callback = callbackOf(start("ticket-1"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		new OAuth2LoginFailureHandler("https://www.example.com")
			.onAuthenticationFailure(callback, response, new OAuth2AuthenticationException("access_denied"));

		assertThat(response.getRedirectedUrl()).isEqualTo("https://www.example.com/?withdrawal=failed");
	}

	private OAuth2AuthorizationRequest start(String ticket) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
		request.setServletPath("/oauth2/authorization/kakao");
		request.setSession(session);
		if (ticket != null) {
			request.setParameter(WithdrawalAuthorizationRequestResolver.WITHDRAW_PARAMETER, ticket);
		}

		OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request);
		assertThat(authorizationRequest).isNotNull();
		repository.saveAuthorizationRequest(authorizationRequest, request, new MockHttpServletResponse());
		return authorizationRequest;
	}

	private MockHttpServletRequest callbackOf(OAuth2AuthorizationRequest authorizationRequest) {
		MockHttpServletRequest callback = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao");
		callback.setSession(session);
		callback.setParameter("code", "code-1");
		callback.setParameter("state", authorizationRequest.getState());

		assertThat(repository.removeAuthorizationRequest(callback, new MockHttpServletResponse())).isNotNull();
		return callback;
	}
}
