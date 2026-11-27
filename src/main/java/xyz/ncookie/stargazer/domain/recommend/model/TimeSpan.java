package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Duration;
import java.time.Instant;

/**
 * 시작과 끝이 있는 시간 구간. 끝은 구간에 들지 않는다.
 */
public record TimeSpan(Instant start, Instant end) {

	public Duration duration() {
		return Duration.between(start, end);
	}
}
