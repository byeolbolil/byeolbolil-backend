package xyz.ncookie.stargazer.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 통합 테스트에서 현재 시각을 정하는 Clock.
 * 시각을 정하지 않으면 실제 시각을 돌려준다. IntegrationTestSupport가 테스트마다 원래대로 되돌린다.
 */
public class TestClock extends Clock {

	private volatile Instant fixed;

	public void setTo(Instant instant) {
		this.fixed = instant;
	}

	public void reset() {
		this.fixed = null;
	}

	@Override
	public Instant instant() {
		Instant current = fixed;
		return (current != null) ? current : Instant.now();
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}
}
