package xyz.ncookie.stargazer.domain.weather.client.metnorway;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.weather.client.WeatherProviderException;
import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * MET Norway Locationforecast 2.0 클라이언트. Open-Meteo가 답하지 못할 때 쓰는 보조 소스다.
 * https://api.met.no/weatherapi/locationforecast/2.0/documentation
 *
 * 이용 약관(https://api.met.no/doc/TermsOfService)이 요구하는 것 가운데 여기서 지키는 것은 두 가지다.
 * - 서비스를 식별할 수 있는 User-Agent를 보낸다. 설정값이 비어 있으면 호출하지 않는다.
 * - 좌표는 소수 넷째 자리 이하로 보낸다. 칸 중심 좌표라 둘째 자리까지만 나간다.
 * Expires 헤더까지 다시 요청하지 않는 것은 받은 쪽(WeatherDomainService)이 캐시 기한으로 지킨다.
 */
@Component
public class MetNorwayClient {

	private static final Duration ONE_HOUR = Duration.ofHours(1);

	private final RestTemplate restTemplate;
	private final String baseUrl;
	private final String userAgent;

	public MetNorwayClient(
		RestTemplate restTemplate,
		@Value("${weather.met-norway.base-url}") String baseUrl,
		@Value("${weather.met-norway.user-agent}") String userAgent
	) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.userAgent = userAgent;
	}

	public MetNorwayForecast fetchForecast(ForecastCell cell) {
		if (!StringUtils.hasText(userAgent)) {
			throw new WeatherProviderException("MET Norway User-Agent가 설정되지 않아 호출하지 않습니다.");
		}

		String url = baseUrl + "/weatherapi/locationforecast/2.0/complete"
			+ "?lat=" + cell.latitudeParam()
			+ "&lon=" + cell.longitudeParam();

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.USER_AGENT, userAgent);

		ResponseEntity<MetNorwayForecastResponse> response;
		try {
			response = restTemplate.exchange(
				url, HttpMethod.GET, new HttpEntity<>(headers), MetNorwayForecastResponse.class);
		} catch (RestClientException e) {
			throw new WeatherProviderException("MET Norway 호출 실패: " + e.getMessage(), e);
		}

		MetNorwayForecastResponse body = response.getBody();
		if (body == null || body.properties() == null) {
			throw new WeatherProviderException("MET Norway가 빈 응답을 돌려줬습니다.");
		}

		List<HourlyWeather> hours = toHours(body.properties().timeseries());
		if (hours.isEmpty()) {
			throw new WeatherProviderException("MET Norway 응답에 쓸 수 있는 시간별 값이 없습니다.");
		}

		WeatherForecast forecast = new WeatherForecast(
			ForecastSource.MET_NORWAY,
			updatedAt(body.properties().meta()),
			gridCoordinate(body.geometry(), 1, cell.latitude()),
			gridCoordinate(body.geometry(), 0, cell.longitude()),
			hours
		);
		return new MetNorwayForecast(forecast, expiresAt(response.getHeaders()));
	}

	/**
	 * 앞쪽의 1시간 간격 구간만 쓴다.
	 * 이 API는 처음 이틀 반쯤은 1시간 간격, 그 뒤는 6시간 간격으로 준다. 간격이 벌어지는 지점에서 멈춘다.
	 */
	private List<HourlyWeather> toHours(List<MetNorwayForecastResponse.TimeStep> timeseries) {
		List<HourlyWeather> hours = new ArrayList<>();
		if (timeseries == null) {
			return hours;
		}

		Instant previous = null;
		for (MetNorwayForecastResponse.TimeStep step : timeseries) {
			Instant time = parse(step.time());
			MetNorwayForecastResponse.Details details = detailsOf(step);
			if (time == null || !isComplete(details)) {
				break;
			}
			if (previous != null && !Duration.between(previous, time).equals(ONE_HOUR)) {
				break;
			}

			hours.add(new HourlyWeather(
				time,
				(int) Math.round(details.cloudAreaFraction()),
				(int) Math.round(details.cloudAreaFractionLow()),
				(int) Math.round(details.cloudAreaFractionMedium()),
				(int) Math.round(details.cloudAreaFractionHigh()),
				(int) Math.round(details.relativeHumidity()),
				details.dewPointTemperature(),
				details.airTemperature(),
				null
			));
			previous = time;
		}
		return hours;
	}

	private static MetNorwayForecastResponse.Details detailsOf(MetNorwayForecastResponse.TimeStep step) {
		if (step.data() == null || step.data().instant() == null) {
			return null;
		}
		return step.data().instant().details();
	}

	private static boolean isComplete(MetNorwayForecastResponse.Details details) {
		return details != null
			&& details.cloudAreaFraction() != null
			&& details.cloudAreaFractionLow() != null
			&& details.cloudAreaFractionMedium() != null
			&& details.cloudAreaFractionHigh() != null
			&& details.relativeHumidity() != null
			&& details.dewPointTemperature() != null
			&& details.airTemperature() != null;
	}

	private static Instant updatedAt(MetNorwayForecastResponse.Meta meta) {
		return (meta != null) ? parse(meta.updatedAt()) : null;
	}

	private static Instant parse(String isoInstant) {
		if (isoInstant == null) {
			return null;
		}
		try {
			return Instant.parse(isoInstant);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static double gridCoordinate(MetNorwayForecastResponse.Geometry geometry, int index, double fallback) {
		if (geometry == null || geometry.coordinates() == null || geometry.coordinates().size() <= index
			|| geometry.coordinates().get(index) == null) {
			return fallback;
		}
		return geometry.coordinates().get(index);
	}

	private static Instant expiresAt(HttpHeaders headers) {
		try {
			long expires = headers.getExpires();
			return (expires >= 0) ? Instant.ofEpochMilli(expires) : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
