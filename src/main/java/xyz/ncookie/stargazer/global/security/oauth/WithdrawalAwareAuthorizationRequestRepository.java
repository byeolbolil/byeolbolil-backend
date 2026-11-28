package xyz.ncookie.stargazer.global.security.oauth;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 기본 저장소(세션)에 맡기되, 콜백에서 꺼낸 인가 요청에 탈퇴 표가 있으면 그 요청의 속성으로 남겨 둔다.
 * 사용자 정보를 읽는 단계와 성공, 실패 처리기는 인가 요청을 직접 받지 못해서 여기서 표를 넘겨준다.
 */
public class WithdrawalAwareAuthorizationRequestRepository
	implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String REQUEST_ATTRIBUTE =
		WithdrawalAwareAuthorizationRequestRepository.class.getName() + ".ticket";

	private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate =
		new HttpSessionOAuth2AuthorizationRequestRepository();

	/** 이 콜백이 탈퇴라면 그 표, 아니면 null */
	public static String withdrawalTicket(HttpServletRequest request) {
		return request == null ? null : (String)request.getAttribute(REQUEST_ATTRIBUTE);
	}

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		return delegate.loadAuthorizationRequest(request);
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
		HttpServletResponse response) {
		delegate.saveAuthorizationRequest(authorizationRequest, request, response);
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
		HttpServletResponse response) {

		OAuth2AuthorizationRequest removed = delegate.removeAuthorizationRequest(request, response);
		if (removed != null
			&& removed.getAttribute(WithdrawalAuthorizationRequestResolver.TICKET_ATTRIBUTE) instanceof String ticket) {
			request.setAttribute(REQUEST_ATTRIBUTE, ticket);
		}
		return removed;
	}
}
