package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Duration;
import java.time.Instant;

/**
 * 맑은 시간이 끊기지 않고 이어지는 구간.
 *
 * @param start 구간이 시작하는 시각
 * @param end   구간이 끝나는 시각. 마지막 맑은 칸의 끝이다
 */
public record ClearWindow(Instant start, Instant end) {

	public int hours() {
		return (int) Duration.between(start, end).toHours();
	}

	public boolean contains(Instant hourStart) {
		return !hourStart.isBefore(start) && hourStart.isBefore(end);
	}
}
