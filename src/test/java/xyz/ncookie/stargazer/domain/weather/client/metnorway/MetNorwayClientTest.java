package xyz.ncookie.stargazer.domain.weather.client.metnorway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.weather.client.WeatherProviderException;
import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;
import xyz.ncookie.stargazer.support.Fixtures;

/**
 * 2026-11-23에 실제로 받아 둔 응답(fixtures/metnorway)으로 확인한다.
 * 그 응답은 18:00부터 26일 06:00까지 1시간 간격 61개, 그 뒤로 6시간 간격 27개가 들어 있다.
 */
class MetNorwayClientTest {

	private static final ForecastCell ANBANDEGI = ForecastCell.of(37.6254, 128.9392);
	private static final String RECORDED = "metnorway/complete-anbandegi-20261123.json";

	private MockRestServiceServer server;

	@Test
	@DisplayName("칸 중심 좌표와 설정한 User-Agent로 complete 예보를 요청한다")
	void request() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(requestTo("https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=37.65&lon=128.95"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.USER_AGENT, "stargazer-test"))
			.andRespond(recorded());

		client.fetchForecast(ANBANDEGI);

		server.verify();
	}

	@Test
	@DisplayName("1시간 간격으로 이어지는 앞쪽 61시간만 쓰고, 6시간 간격으로 바뀌는 곳에서 멈춘다")
	void keepsOnlyHourlyPart() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(recorded());

		WeatherForecast forecast = client.fetchForecast(ANBANDEGI).forecast();

		assertThat(forecast.hours()).hasSize(61);
		assertThat(forecast.hours().get(0).time()).isEqualTo(Instant.parse("2026-11-23T18:00:00Z"));
		assertThat(forecast.hours().get(60).time()).isEqualTo(Instant.parse("2026-11-26T06:00:00Z"));
		for (int i = 1; i < forecast.hours().size(); i++) {
			assertThat(forecast.hours().get(i).time())
				.isEqualTo(forecast.hours().get(i - 1).time().plus(1, ChronoUnit.HOURS));
		}
	}

	@Test
	@DisplayName("소수로 온 운량과 습도는 반올림하고, 시정은 주지 않으므로 null로 둔다")
	void readsValues() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(recorded());

		WeatherForecast forecast = client.fetchForecast(ANBANDEGI).forecast();

		// 원래 값: 총운량 68.7, 하층 41.4, 중층 0.0, 상층 43.7, 습도 60.1
		assertThat(forecast.hours().get(0)).isEqualTo(new HourlyWeather(
			Instant.parse("2026-11-23T18:00:00Z"), 69, 41, 0, 44, 60, -5.6, 1.6, null));
		// 원래 값: 총운량 79.7, 하층 6.2, 중층 0.0, 상층 78.1, 습도 44.6
		assertThat(forecast.hours().get(60)).isEqualTo(new HourlyWeather(
			Instant.parse("2026-11-26T06:00:00Z"), 80, 6, 0, 78, 45, 0.2, 12.1, null));
	}

	@Test
	@DisplayName("출처, 갱신 시각, 응답이 알려 준 좌표를 싣는다")
	void metadata() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(recorded());

		WeatherForecast forecast = client.fetchForecast(ANBANDEGI).forecast();

		assertThat(forecast.source()).isEqualTo(ForecastSource.MET_NORWAY);
		assertThat(forecast.issuedAt()).isEqualTo(Instant.parse("2026-11-23T17:19:38Z"));
		assertThat(forecast.gridLatitude()).isEqualTo(37.65);
		assertThat(forecast.gridLongitude()).isEqualTo(128.95);
	}

	@Test
	@DisplayName("Expires 헤더를 다시 요청하면 안 되는 기한으로 돌려준다")
	void readsExpires() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(recorded().header(HttpHeaders.EXPIRES, "Mon, 23 Nov 2026 19:18:57 GMT"));

		MetNorwayForecast fetched = client.fetchForecast(ANBANDEGI);

		assertThat(fetched.expiresAt()).isEqualTo(Instant.parse("2026-11-23T19:18:57Z"));
	}

	@Test
	@DisplayName("Expires 헤더가 없으면 기한은 null이다")
	void missingExpires() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(recorded());

		assertThat(client.fetchForecast(ANBANDEGI).expiresAt()).isNull();
	}

	@Test
	@DisplayName("User-Agent가 설정되지 않았으면 외부로 나가지 않고 예외를 던진다")
	void blankUserAgent() {
		MetNorwayClient client = clientWith("");

		assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI)).isInstanceOf(WeatherProviderException.class);
		// 등록한 응답이 없으므로 호출이 나갔다면 AssertionError로 끝났을 것이다.
		server.verify();
	}

	@Test
	@DisplayName("서버 오류는 예외로 던진다")
	void serverError() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(withServerError());

		assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI)).isInstanceOf(WeatherProviderException.class);
	}

	@Test
	@DisplayName("User-Agent를 거부하는 403도 예외로 던진다")
	void forbidden() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI)).isInstanceOf(WeatherProviderException.class);
	}

	@Test
	@DisplayName("시간별 값이 없는 응답은 예외로 던진다")
	void emptyTimeseries() {
		MetNorwayClient client = clientWith("stargazer-test");
		server.expect(anything()).andRespond(withSuccess(
			"{\"properties\": {\"meta\": {\"updated_at\": \"2026-11-23T17:19:38Z\"}, \"timeseries\": []}}",
			MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.fetchForecast(ANBANDEGI)).isInstanceOf(WeatherProviderException.class);
	}

	private MetNorwayClient clientWith(String userAgent) {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		return new MetNorwayClient(restTemplate, "https://api.met.no", userAgent);
	}

	private static DefaultResponseCreator recorded() {
		return withSuccess(Fixtures.read(RECORDED), MediaType.APPLICATION_JSON);
	}
}
