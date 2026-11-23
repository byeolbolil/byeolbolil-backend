package xyz.ncookie.stargazer.domain.weather.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeatherForecastTest {

	private static final Instant MIDNIGHT = Instant.parse("2026-11-23T00:00:00Z");

	@Test
	@DisplayName("기준 시각 앞의 시간은 버리고, 기준 시각 자체는 남긴다")
	void dropsHoursBeforeStart() {
		WeatherForecast forecast = forecastOf(24);

		WeatherForecast window = forecast.startingAt(Instant.parse("2026-11-23T18:00:00Z"), 168);

		assertThat(window.hours()).hasSize(6);
		assertThat(window.hours().get(0).time()).isEqualTo(Instant.parse("2026-11-23T18:00:00Z"));
	}

	@Test
	@DisplayName("남길 시간 수를 넘으면 앞에서부터 그만큼만 남긴다")
	void limitsLength() {
		WeatherForecast forecast = forecastOf(216);

		WeatherForecast window = forecast.startingAt(Instant.parse("2026-11-23T18:00:00Z"), 168);

		assertThat(window.hours()).hasSize(168);
		assertThat(window.hours().get(167).time()).isEqualTo(Instant.parse("2026-11-30T17:00:00Z"));
	}

	@Test
	@DisplayName("기준 시각이 예보 끝보다 뒤면 비어 있다")
	void emptyWhenAllInPast() {
		WeatherForecast forecast = forecastOf(24);

		assertThat(forecast.startingAt(Instant.parse("2026-11-24T00:00:00Z"), 168).hours()).isEmpty();
	}

	@Test
	@DisplayName("출처, 만들어진 시각, 격자점 좌표는 그대로 둔다")
	void keepsMetadata() {
		WeatherForecast forecast = forecastOf(24);

		WeatherForecast window = forecast.startingAt(Instant.parse("2026-11-23T18:00:00Z"), 168);

		assertThat(window.source()).isEqualTo(ForecastSource.OPEN_METEO);
		assertThat(window.issuedAt()).isEqualTo(Instant.parse("2026-11-23T06:00:00Z"));
		assertThat(window.gridLatitude()).isEqualTo(37.64499);
		assertThat(window.gridLongitude()).isEqualTo(128.93192);
	}

	private static WeatherForecast forecastOf(int hourCount) {
		List<HourlyWeather> hours = IntStream.range(0, hourCount)
			.mapToObj(i -> new HourlyWeather(MIDNIGHT.plus(i, ChronoUnit.HOURS), 0, 0, 0, 0, 50, -5.0, 1.0, 20000))
			.toList();
		return new WeatherForecast(
			ForecastSource.OPEN_METEO, Instant.parse("2026-11-23T06:00:00Z"), 37.64499, 128.93192, hours);
	}
}
