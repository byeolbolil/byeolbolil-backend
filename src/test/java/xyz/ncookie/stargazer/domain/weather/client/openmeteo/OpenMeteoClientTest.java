package xyz.ncookie.stargazer.domain.weather.client.openmeteo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.weather.client.WeatherProviderException;
import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;
import xyz.ncookie.stargazer.domain.weather.model.ModelRun;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;
import xyz.ncookie.stargazer.support.Fixtures;

/**
 * 2026-11-23에 실제로 받아 둔 응답(fixtures/openmeteo)으로 확인한다.
 */
class OpenMeteoClientTest {

	private static final ForecastCell ANBANDEGI = ForecastCell.of(37.6254, 128.9392);
	private static final Instant MODEL_RUN = Instant.parse("2026-11-23T06:00:00Z");

	private MockRestServiceServer server;
	private OpenMeteoClient client;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		client = new OpenMeteoClient(restTemplate, "https://api.open-meteo.com", "ecmwf_ifs");
	}

	@Nested
	@DisplayName("시간별 예보")
	class Forecast {

		@Test
		@DisplayName("칸 중심 좌표로 필요한 변수 여덟 개를 UTC 초 단위 시각과 함께 9일치 요청한다")
		void request() {
			server.expect(requestTo("https://api.open-meteo.com/v1/forecast"
					+ "?latitude=37.65&longitude=128.95"
					+ "&hourly=cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,"
					+ "relative_humidity_2m,dew_point_2m,temperature_2m,visibility"
					+ "&forecast_days=9&timeformat=unixtime&timezone=GMT&models=ecmwf_ifs"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(json("openmeteo/forecast-ecmwf-ifs-anbandegi-20261123.json"));

			client.fetchForecast(ANBANDEGI, MODEL_RUN);

			server.verify();
		}

		@Test
		@DisplayName("9일치 216시간을 1시간 간격으로 읽는다")
		void readsAllHours() {
			server.expect(anything()).andRespond(json("openmeteo/forecast-ecmwf-ifs-anbandegi-20261123.json"));

			WeatherForecast forecast = client.fetchForecast(ANBANDEGI, MODEL_RUN);

			assertThat(forecast.hours()).hasSize(216);
			assertThat(forecast.hours().get(0).time()).isEqualTo(Instant.parse("2026-11-23T00:00:00Z"));
			assertThat(forecast.hours().get(215).time()).isEqualTo(Instant.parse("2026-12-01T23:00:00Z"));
			for (int i = 1; i < forecast.hours().size(); i++) {
				assertThat(forecast.hours().get(i).time())
					.isEqualTo(forecast.hours().get(i - 1).time().plus(1, ChronoUnit.HOURS));
			}
		}

		@Test
		@DisplayName("한 시각의 운량, 습도, 이슬점, 기온, 시정을 응답 값 그대로 읽는다")
		void readsValues() {
			server.expect(anything()).andRespond(json("openmeteo/forecast-ecmwf-ifs-anbandegi-20261123.json"));

			WeatherForecast forecast = client.fetchForecast(ANBANDEGI, MODEL_RUN);

			assertThat(forecast.hours().get(18)).isEqualTo(new HourlyWeather(
				Instant.parse("2026-11-23T18:00:00Z"), 71, 65, 0, 18, 65, -4.3, 1.5, 34200));
			assertThat(forecast.hours().get(215)).isEqualTo(new HourlyWeather(
				Instant.parse("2026-12-01T23:00:00Z"), 99, 99, 0, 79, 90, 10.8, 12.4, 3940));
		}

		@Test
		@DisplayName("출처, 넘겨받은 모델 실행 시각, 응답이 알려 준 격자점 좌표를 싣는다")
		void metadata() {
			server.expect(anything()).andRespond(json("openmeteo/forecast-ecmwf-ifs-anbandegi-20261123.json"));

			WeatherForecast forecast = client.fetchForecast(ANBANDEGI, MODEL_RUN);

			assertThat(forecast.source()).isEqualTo(ForecastSource.OPEN_METEO);
			assertThat(forecast.issuedAt()).isEqualTo(MODEL_RUN);
			assertThat(forecast.gridLatitude()).isEqualTo(37.64499);
			assertThat(forecast.gridLongitude()).isEqualTo(128.93192);
		}

		@Test
		@DisplayName("값이 비는 시각을 만나면 그 앞까지만 쓴다")
		void stopsAtFirstMissingValue() {
			server.expect(anything()).andRespond(withSuccess("""
				{
				  "latitude": 37.64499, "longitude": 128.93192,
				  "hourly": {
				    "time": [1795392000, 1795395600, 1795399200, 1795402800],
				    "cloud_cover": [10, 20, null, 40],
				    "cloud_cover_low": [1, 2, 3, 4],
				    "cloud_cover_mid": [1, 2, 3, 4],
				    "cloud_cover_high": [1, 2, 3, 4],
				    "relative_humidity_2m": [50, 50, 50, 50],
				    "dew_point_2m": [-1.0, -1.0, -1.0, -1.0],
				    "temperature_2m": [1.0, 1.0, 1.0, 1.0],
				    "visibility": [20000.0, 20000.0, 20000.0, 20000.0]
				  }
				}
				""", MediaType.APPLICATION_JSON));

			WeatherForecast forecast = client.fetchForecast(ANBANDEGI, MODEL_RUN);

			assertThat(forecast.hours()).extracting(HourlyWeather::cloudCover).containsExactly(10, 20);
		}

		@Test
		@DisplayName("시정만 비는 시각은 시정을 null로 두고 계속 읽는다")
		void missingVisibilityIsKept() {
			server.expect(anything()).andRespond(withSuccess("""
				{
				  "latitude": 37.64499, "longitude": 128.93192,
				  "hourly": {
				    "time": [1795392000, 1795395600],
				    "cloud_cover": [10, 20],
				    "cloud_cover_low": [1, 2],
				    "cloud_cover_mid": [1, 2],
				    "cloud_cover_high": [1, 2],
				    "relative_humidity_2m": [50, 50],
				    "dew_point_2m": [-1.0, -1.0],
				    "temperature_2m": [1.0, 1.0],
				    "visibility": [null, 20000.0]
				  }
				}
				""", MediaType.APPLICATION_JSON));

			WeatherForecast forecast = client.fetchForecast(ANBANDEGI, MODEL_RUN);

			assertThat(forecast.hours()).extracting(HourlyWeather::visibility).containsExactly(null, 20000);
		}

		@Test
		@DisplayName("서버 오류는 대체값 없이 예외로 던진다")
		void serverError() {
			server.expect(anything()).andRespond(withServerError());

			assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI, MODEL_RUN))
				.isInstanceOf(WeatherProviderException.class);
		}

		@Test
		@DisplayName("잘못된 요청이라는 400 응답도 예외로 던진다")
		void badRequest() {
			server.expect(anything()).andRespond(withBadRequest()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":true,\"reason\":\"Invalid value: Cannot initialize MultiDomains from invalid String value nope\"}"));

			assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI, MODEL_RUN))
				.isInstanceOf(WeatherProviderException.class);
		}

		@Test
		@DisplayName("시간별 값이 하나도 없는 응답은 예외로 던진다")
		void emptyHourly() {
			server.expect(anything()).andRespond(withSuccess(
				"{\"latitude\": 37.64499, \"longitude\": 128.93192}", MediaType.APPLICATION_JSON));

			assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI, MODEL_RUN))
				.isInstanceOf(WeatherProviderException.class);
		}
	}

	@Nested
	@DisplayName("모델 실행 시각")
	class LatestRun {

		@Test
		@DisplayName("모델 메타에서 실행 기준 시각과 갱신 주기를 읽는다")
		void readsRun() {
			server.expect(requestTo("https://api.open-meteo.com/data/ecmwf_ifs/static/meta.json"))
				.andRespond(json("openmeteo/meta-ecmwf-ifs-20261123.json"));

			ModelRun run = client.fetchLatestRun();

			assertThat(run.initialisedAt()).isEqualTo(Instant.parse("2026-11-23T06:00:00Z"));
			assertThat(run.updateInterval()).isEqualTo(Duration.ofHours(6));
		}

		@Test
		@DisplayName("서버 오류는 예외로 던진다")
		void serverError() {
			server.expect(anything()).andRespond(withServerError());

			assertThatThrownBy(() -> client.fetchLatestRun()).isInstanceOf(WeatherProviderException.class);
		}

		@Test
		@DisplayName("실행 시각이 빠진 응답은 예외로 던진다")
		void missingRunTime() {
			server.expect(anything()).andRespond(withSuccess(
				"{\"update_interval_seconds\": 21600}", MediaType.APPLICATION_JSON));

			assertThatThrownBy(() -> client.fetchLatestRun()).isInstanceOf(WeatherProviderException.class);
		}
	}

	private static org.springframework.test.web.client.ResponseCreator json(String fixture) {
		return withSuccess(Fixtures.read(fixture), MediaType.APPLICATION_JSON);
	}
}
