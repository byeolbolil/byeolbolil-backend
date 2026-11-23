package xyz.ncookie.stargazer.domain.weather.model;

import java.time.Instant;
import java.util.List;

/**
 * 한 지점의 시간별 예보.
 *
 * hours는 1시간 간격으로 빠짐없이 이어진다. 맑은 시간이 몇 시간 이어지는지 셀 때 이 전제를 쓴다.
 *
 * @param source        예보를 받아 온 곳
 * @param issuedAt      예보가 만들어진 시각. Open-Meteo는 모델 실행 시각, MET Norway는 갱신 시각이다.
 *                      모델 실행 시각을 확인하지 못했으면 null이다
 * @param gridLatitude  예보가 실제로 계산된 격자점의 위도
 * @param gridLongitude 예보가 실제로 계산된 격자점의 경도
 * @param hours         시간별 값
 */
public record WeatherForecast(
	ForecastSource source,
	Instant issuedAt,
	double gridLatitude,
	double gridLongitude,
	List<HourlyWeather> hours
) {

	public WeatherForecast {
		hours = List.copyOf(hours);
	}

	/**
	 * from 시각부터 최대 maxHours 시간만 남긴다.
	 */
	public WeatherForecast startingAt(Instant from, int maxHours) {
		List<HourlyWeather> window = hours.stream()
			.filter(hour -> !hour.time().isBefore(from))
			.limit(maxHours)
			.toList();
		return new WeatherForecast(source, issuedAt, gridLatitude, gridLongitude, window);
	}
}
