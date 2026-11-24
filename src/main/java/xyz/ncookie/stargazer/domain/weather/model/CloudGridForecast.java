package xyz.ncookie.stargazer.domain.weather.model;

import java.time.Instant;
import java.util.List;

/**
 * 격자 전체의 시간별 총운량 예보. 지도에 구름이 어디에 끼는지 그리는 데 쓴다.
 *
 * @param source   예보를 받아 온 곳
 * @param issuedAt 모델 실행 시각. 확인하지 못했으면 null이다
 * @param grid     격자
 * @param frames   한 시간 간격으로 이어지는 값
 */
public record CloudGridForecast(
	ForecastSource source,
	Instant issuedAt,
	CloudGridSpec grid,
	List<Frame> frames
) {

	public CloudGridForecast {
		frames = List.copyOf(frames);
	}

	/**
	 * 한 시각의 격자 값.
	 *
	 * @param time       이 시각부터 1시간
	 * @param cloudCover 총운량(%). 남쪽 줄부터, 줄 안에서는 서쪽부터 적는다
	 */
	public record Frame(Instant time, List<Integer> cloudCover) {

		public Frame {
			cloudCover = List.copyOf(cloudCover);
		}
	}

	/**
	 * from부터 until 전까지의 시각만 남긴다.
	 */
	public CloudGridForecast between(Instant from, Instant until) {
		List<Frame> window = frames.stream()
			.filter(frame -> !frame.time().isBefore(from) && frame.time().isBefore(until))
			.toList();
		return new CloudGridForecast(source, issuedAt, grid, window);
	}
}
