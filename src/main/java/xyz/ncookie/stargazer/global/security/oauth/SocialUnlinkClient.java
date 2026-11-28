package xyz.ncookie.stargazer.global.security.oauth;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;

/**
 * 탈퇴할 때 제공자에게 앱 연결을 끊어 달라고 한다. 끊어 두면 같은 계정으로 다시 들어올 때 동의 화면부터 다시 거친다.
 * 탈퇴 직전에 받은 그 회원의 액세스 토큰으로 부르므로 제공자 토큰을 따로 보관하지 않는다.
 *
 * - 카카오 연결 끊기: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#unlink
 * - 네이버 연동 해제(grant_type=delete): https://developers.naver.com/docs/login/devguide/devguide.md
 * - 구글 토큰 취소: https://developers.google.com/identity/protocols/oauth2/web-server#tokenrevoke
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUnlinkClient {

	static final String KAKAO_UNLINK = "https://kapi.kakao.com/v1/user/unlink";
	static final String NAVER_TOKEN = "https://nid.naver.com/oauth2.0/token";
	static final String GOOGLE_REVOKE = "https://oauth2.googleapis.com/revoke";

	private final RestTemplate restTemplate;
	private final ClientRegistrationRepository clientRegistrationRepository;

	/**
	 * 연결을 끊었으면 true. 실패해도 예외를 던지지 않는다. 탈퇴는 계속 진행하고, 남은 연결은 회원이 제공자 쪽
	 * 설정에서 끊을 수 있다.
	 */
	public boolean unlink(AuthProvider provider, String accessToken) {

		try {
			return switch (provider) {
				case KAKAO -> unlinkKakao(accessToken);
				case NAVER -> unlinkNaver(accessToken);
				case GOOGLE -> revokeGoogle(accessToken);
				case LOCAL -> false;
			};
		} catch (Exception e) {
			log.warn("{} 연결 끊기 실패: {}", provider, e.getMessage());
			return false;
		}
	}

	private boolean unlinkKakao(String accessToken) {

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		restTemplate.postForEntity(KAKAO_UNLINK, new HttpEntity<>(headers), String.class);
		return true;
	}

	/** 네이버는 실패해도 200으로 답하고 본문에 error를 싣는다 */
	private boolean unlinkNaver(String accessToken) {

		ClientRegistration naver = clientRegistrationRepository.findByRegistrationId("naver");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "delete");
		form.add("client_id", naver.getClientId());
		form.add("client_secret", naver.getClientSecret());
		form.add("access_token", accessToken);
		form.add("service_provider", "NAVER");

		Map<?, ?> body = restTemplate.postForObject(NAVER_TOKEN, formEntity(form), Map.class);
		if (body == null || !"success".equals(body.get("result"))) {
			log.warn("네이버 연동 해제 실패: {}", body == null ? null : body.get("error"));
			return false;
		}
		return true;
	}

	private boolean revokeGoogle(String accessToken) {

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("token", accessToken);
		restTemplate.postForEntity(GOOGLE_REVOKE, formEntity(form), String.class);
		return true;
	}

	private static HttpEntity<MultiValueMap<String, String>> formEntity(MultiValueMap<String, String> form) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		return new HttpEntity<>(form, headers);
	}
}
