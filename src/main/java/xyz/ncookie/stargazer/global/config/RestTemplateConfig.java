package xyz.ncookie.stargazer.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	// 외부 API가 응답하지 않을 때 요청 스레드가 무한정 붙잡히지 않게 한다.
	// 날씨 예보는 이 시간 안에 답이 없으면 보조 소스로 넘어간다. 값을 고른 이유는 docs/decisions.md에 있다.
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return new RestTemplate(requestFactory);
	}
}
