package xyz.ncookie.stargazer.domain.weather.client.metnorway;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MET Norway Locationforecast 2.0 complete 응답(GeoJSON) 가운데 쓰는 부분.
 * geometry.coordinates는 [경도, 위도, 고도] 순서다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetNorwayForecastResponse(
	Geometry geometry,
	Properties properties
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Geometry(List<Double> coordinates) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Properties(Meta meta, List<TimeStep> timeseries) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Meta(@JsonProperty("updated_at") String updatedAt) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TimeStep(String time, Data data) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Data(InstantData instant) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InstantData(Details details) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Details(
		@JsonProperty("cloud_area_fraction") Double cloudAreaFraction,
		@JsonProperty("cloud_area_fraction_low") Double cloudAreaFractionLow,
		@JsonProperty("cloud_area_fraction_medium") Double cloudAreaFractionMedium,
		@JsonProperty("cloud_area_fraction_high") Double cloudAreaFractionHigh,
		@JsonProperty("relative_humidity") Double relativeHumidity,
		@JsonProperty("dew_point_temperature") Double dewPointTemperature,
		@JsonProperty("air_temperature") Double airTemperature
	) {}
}
