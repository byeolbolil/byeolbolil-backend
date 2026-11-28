package xyz.ncookie.stargazer.global.security.oauth;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 소셜 로그인 주소에 {@code ?withdraw=탈퇴 표}가 붙어 오면 그 표를 인가 요청에 실어 둔다.
 * 제공자에서 돌아온 콜백이 이 요청을 꺼낼 때 표가 함께 나오고, 그러면 로그인 대신 탈퇴로 처리한다.
 */
public class WithdrawalAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

	public static final String WITHDRAW_PARAMETER = "withdraw";
	static final String TICKET_ATTRIBUTE = "withdrawal_ticket";

	private final DefaultOAuth2AuthorizationRequestResolver delegate;

	public WithdrawalAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
		this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		return withTicket(request, delegate.resolve(request));
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		return withTicket(request, delegate.resolve(request, clientRegistrationId));
	}

	private static OAuth2AuthorizationRequest withTicket(HttpServletRequest request,
		OAuth2AuthorizationRequest authorizationRequest) {

		String ticket = request.getParameter(WITHDRAW_PARAMETER);
		if (authorizationRequest == null || ticket == null || ticket.isBlank()) {
			return authorizationRequest;
		}
		return OAuth2AuthorizationRequest.from(authorizationRequest)
			.attributes(attributes -> attributes.put(TICKET_ATTRIBUTE, ticket))
			.build();
	}
}
