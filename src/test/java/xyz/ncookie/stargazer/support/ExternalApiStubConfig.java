package xyz.ncookie.stargazer.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration(proxyBeanMethods = false)
public class ExternalApiStubConfig {

	@Bean
	ExternalApiStubs externalApiStubs(RestTemplate restTemplate) {
		return new ExternalApiStubs(restTemplate);
	}
}
