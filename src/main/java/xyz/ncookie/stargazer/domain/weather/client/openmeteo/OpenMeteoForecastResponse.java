package xyz.ncookie.stargazer.domain.weather.client.openmeteo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Open-Meteo /v1/forecast 응답 가운데 쓰는 부분.
 * hourly의 각 목록은 time과 같은 길이이고, 모델이 값을 주지 않은 시각은 null이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoForecastResponse(
	Double latitude,
	Double longitude,
	Hourly hourly
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Hourly(
		List<Long> time,
		@JsonProperty("cloud_cover") List<Double> cloudCover,
		@JsonProperty("cloud_cover_low") List<Double> cloudCoverLow,
		@JsonProperty("cloud_cover_mid") List<Double> cloudCoverMid,
		@JsonProperty("cloud_cover_high") List<Double> cloudCoverHigh,
		@JsonProperty("relative_humidity_2m") List<Double> relativeHumidity,
		@JsonProperty("dew_point_2m") List<Double> dewPoint,
		@JsonProperty("temperature_2m") List<Double> temperature,
		List<Double> visibility
	) {}
}
