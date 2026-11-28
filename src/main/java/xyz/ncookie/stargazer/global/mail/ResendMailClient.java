package xyz.ncookie.stargazer.global.mail;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Resend로 메일을 보낸다. https://resend.com/docs/api-reference/emails/send-email
 *
 * 키가 비어 있으면 보내지 않고 본문을 로그에만 남긴다. 로컬에서 가입을 시험할 때 인증 코드를 로그에서 읽기 위한 것이다.
 * 운영은 배포가 GitHub secret의 RESEND_API_KEY를 넣어 준다.
 */
@Slf4j
@Component
public class ResendMailClient {

	private final RestTemplate restTemplate;
	private final String baseUrl;
	private final String apiKey;
	private final String from;

	public ResendMailClient(
		RestTemplate restTemplate,
		@Value("${mail.resend.base-url}") String baseUrl,
		@Value("${mail.resend.api-key}") String apiKey,
		@Value("${mail.from}") String from
	) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey == null ? "" : apiKey.strip();
		this.from = from;
	}

	public void send(String to, String subject, String text) {

		if (apiKey.isEmpty()) {
			log.warn("메일 발송 키가 없어 보내지 않고 로그에만 남긴다. to={}, subject={}, text={}", to, subject, text);
			return;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> body = Map.of("from", from, "to", List.of(to), "subject", subject, "text", text);

		try {
			restTemplate.postForEntity(baseUrl + "/emails", new HttpEntity<>(body, headers), String.class);
		} catch (RestClientException e) {
			throw new MailSendException(e);
		}
	}

	public static class MailSendException extends RuntimeException {

		public MailSendException(Throwable cause) {
			super("메일을 보내지 못했습니다", cause);
		}
	}
}
