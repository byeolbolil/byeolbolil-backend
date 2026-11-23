package xyz.ncookie.stargazer.domain.weather.client.openmeteo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.weather.client.WeatherProviderException;
import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;
import xyz.ncookie.stargazer.domain.weather.model.ModelRun;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * Open-Meteo 예보 API 클라이언트. 키 없이 쓰는 무료 API다.
 * https://open-meteo.com/en/docs
 *
 * 실패는 모두 WeatherProviderException으로 던진다. 대체값을 만들어 돌려주지 않는다.
 */
@Component
public class OpenMeteoClient {

	private static final String HOURLY_VARIABLES = String.join(",",
		"cloud_cover",
		"cloud_cover_low",
		"cloud_cover_mid",
		"cloud_cover_high",
		"relative_humidity_2m",
		"dew_point_2m",
		"temperature_2m",
		"visibility"
	);

	// 응답은 요청한 날의 00:00(UTC)부터 시작한다. 받아 둔 예보를 다음 모델 실행이 나올 때까지 쓰므로,
	// 하루가 지난 뒤에 읽어도 7일(168시간)이 남도록 9일치를 받는다.
	private static final int FORECAST_DAYS = 9;

	private final RestTemplate restTemplate;
	private final String baseUrl;
	private final String model;

	public OpenMeteoClient(
		RestTemplate restTemplate,
		@Value("${weather.open-meteo.base-url}") String baseUrl,
		@Value("${weather.open-meteo.model}") String model
	) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.model = model;
	}

	public String model() {
		return model;
	}

	/**
	 * 지금 내주고 있는 모델 실행을 조회한다.
	 */
	public ModelRun fetchLatestRun() {
		String url = baseUrl + "/data/" + model + "/static/meta.json";

		OpenMeteoModelMetaResponse meta = get(url, OpenMeteoModelMetaResponse.class);

		if (meta.lastRunInitialisationTime() == null || meta.updateIntervalSeconds() == null) {
			throw new WeatherProviderException("Open-Meteo 모델 메타에 실행 시각이 없습니다: " + model);
		}
		return new ModelRun(
			Instant.ofEpochSecond(meta.lastRunInitialisationTime()),
			Duration.ofSeconds(meta.updateIntervalSeconds())
		);
	}

	/**
	 * 칸 중심 좌표의 시간별 예보를 받는다.
	 *
	 * @param issuedAt 결과에 실어 둘 모델 실행 시각. 모르면 null
	 */
	public WeatherForecast fetchForecast(ForecastCell cell, Instant issuedAt) {
		String url = baseUrl + "/v1/forecast"
			+ "?latitude=" + cell.latitudeParam()
			+ "&longitude=" + cell.longitudeParam()
			+ "&hourly=" + HOURLY_VARIABLES
			+ "&forecast_days=" + FORECAST_DAYS
			+ "&timeformat=unixtime"
			+ "&timezone=GMT"
			+ "&models=" + model;

		OpenMeteoForecastResponse response = get(url, OpenMeteoForecastResponse.class);

		List<HourlyWeather> hours = toHours(response.hourly());
		if (hours.isEmpty()) {
			throw new WeatherProviderException("Open-Meteo 응답에 쓸 수 있는 시간별 값이 없습니다.");
		}

		return new WeatherForecast(
			ForecastSource.OPEN_METEO,
			issuedAt,
			response.latitude() != null ? response.latitude() : cell.latitude(),
			response.longitude() != null ? response.longitude() : cell.longitude(),
			hours
		);
	}

	private <T> T get(String url, Class<T> type) {
		T body;
		try {
			body = restTemplate.getForObject(url, type);
		} catch (RestClientException e) {
			throw new WeatherProviderException("Open-Meteo 호출 실패: " + e.getMessage(), e);
		}
		if (body == null) {
			throw new WeatherProviderException("Open-Meteo가 빈 응답을 돌려줬습니다.");
		}
		return body;
	}

	/**
	 * 목록 여러 개로 온 값을 시각별로 묶는다.
	 * 시정을 뺀 값 가운데 하나라도 비는 시각을 만나면 거기서 멈춘다. 뒤쪽을 건너뛰어 이으면 시간이 끊기기 때문이다.
	 */
	private List<HourlyWeather> toHours(OpenMeteoForecastResponse.Hourly hourly) {
		List<HourlyWeather> hours = new ArrayList<>();
		if (hourly == null || hourly.time() == null) {
			return hours;
		}

		for (int i = 0; i < hourly.time().size(); i++) {
			Long time = hourly.time().get(i);
			Double cloudCover = at(hourly.cloudCover(), i);
			Double cloudCoverLow = at(hourly.cloudCoverLow(), i);
			Double cloudCoverMid = at(hourly.cloudCoverMid(), i);
			Double cloudCoverHigh = at(hourly.cloudCoverHigh(), i);
			Double relativeHumidity = at(hourly.relativeHumidity(), i);
			Double dewPoint = at(hourly.dewPoint(), i);
			Double temperature = at(hourly.temperature(), i);
			Double visibility = at(hourly.visibility(), i);

			if (time == null || cloudCover == null || cloudCoverLow == null || cloudCoverMid == null
				|| cloudCoverHigh == null || relativeHumidity == null || dewPoint == null || temperature == null) {
				break;
			}

			hours.add(new HourlyWeather(
				Instant.ofEpochSecond(time),
				(int) Math.round(cloudCover),
				(int) Math.round(cloudCoverLow),
				(int) Math.round(cloudCoverMid),
				(int) Math.round(cloudCoverHigh),
				(int) Math.round(relativeHumidity),
				dewPoint,
				temperature,
				visibility != null ? (int) Math.round(visibility) : null
			));
		}
		return hours;
	}

	private static Double at(List<Double> values, int index) {
		return (values != null && index < values.size()) ? values.get(index) : null;
	}
}
