package xyz.ncookie.stargazer.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각을 주입받아 쓰기 위한 Clock.
 * 예보는 "지금부터 몇 시간"을 다루므로, 시각을 고정할 수 있어야 테스트가 기록해 둔 응답과 맞는다.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
