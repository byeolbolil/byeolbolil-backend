package xyz.ncookie.stargazer.domain.stargazing.client.openweather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import io.github.bucket4j.Bucket;
import xyz.ncookie.stargazer.global.exception.RateLimitExceededException;
import xyz.ncookie.stargazer.global.util.WeatherDataFactory;
import xyz.ncookie.stargazer.support.Fixtures;

/**
 * OpenWeather 응답을 읽는 방식, 실패했을 때 돌려주는 값, 호출 제한을 고정한다.
 * 스프링 없이 객체를 직접 만들므로 @Cacheable은 걸리지 않는다. 캐시를 거친 동작은 StargazingApiTest가 본다.
 */
class OpenWeatherMapClientTest {

	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;

	// clientWith()가 테스트마다 새로 묶는다.
	private MockRestServiceServer server;

	@Test
	@DisplayName("현재 날씨에서 구름량, 습도, 시정을 읽는다")
	void readsCurrentWeather() {
		OpenWeatherMapClient client = clientWith(bucketOf(10));
		server.expect(requestTo(allOf(
				startsWith("https://api.openweathermap.org/data/2.5/weather?"),
				containsString("appid=test-weather-key"),
				containsString("units=metric"))))
			.andRespond(withSuccess(Fixtures.read("openweather/current-clear.json"), MediaType.APPLICATION_JSON));

		OpenWeatherResponse weather = client.fetchCurrentWeatherApi(LAT, LON);

		assertThat(weather.clouds().all()).isEqualTo(5);
		assertThat(weather.main().humidity()).isEqualTo(45.0);
		assertThat(weather.visibility()).isEqualTo(10000);
		server.verify();
	}

	@Test
	@DisplayName("예보에서 칸마다 시각, 구름량, 습도, 시정을 읽는다")
	void readsForecast() {
		OpenWeatherMapClient client = clientWith(bucketOf(10));
		server.expect(requestTo(startsWith("https://api.openweathermap.org/data/2.5/forecast?")))
			.andRespond(withSuccess(
				Fixtures.read("openweather/forecast-anbandegi-20260115.json"), MediaType.APPLICATION_JSON));

		OpenWeatherForecastResponse forecast = client.fetchForecastApi(LAT, LON);

		assertThat(forecast.list()).hasSize(6);
		OpenWeatherForecastResponse.Item first = forecast.list().get(0);
		assertThat(first.dt()).isEqualTo(1768467600L);
		assertThat(first.clouds().all()).isEqualTo(40);
		assertThat(first.main().humidity()).isEqualTo(70.0);
		assertThat(first.visibility()).isEqualTo(10000);
	}

	@Test
	@DisplayName("현재 날씨 호출이 실패하면 예외 대신 구름 100%인 대체 날씨를 돌려준다")
	void currentFailureBecomesOvercast() {
		OpenWeatherMapClient client = clientWith(bucketOf(10));
		server.expect(anything()).andRespond(withServerError());

		OpenWeatherResponse weather = client.fetchCurrentWeatherApi(LAT, LON);

		assertThat(weather.clouds().all()).isEqualTo(100);
		assertThat(weather.main().humidity()).isEqualTo(50.0);
		assertThat(weather.visibility()).isEqualTo(10000);
	}

	@Test
	@DisplayName("예보 호출이 실패하면 예외 대신 null을 돌려준다")
	void forecastFailureBecomesNull() {
		OpenWeatherMapClient client = clientWith(bucketOf(10));
		server.expect(anything()).andRespond(withServerError());

		assertThat(client.fetchForecastApi(LAT, LON)).isNull();
	}

	@Test
	@DisplayName("호출 제한 토큰이 떨어지면 외부로 나가지 않고 예외를 던진다")
	void rateLimitExceeded() {
		OpenWeatherMapClient client = clientWith(bucketOf(1));
		server.expect(anything())
			.andRespond(withSuccess(Fixtures.read("openweather/current-clear.json"), MediaType.APPLICATION_JSON));

		client.fetchCurrentWeatherApi(LAT, LON);

		assertThatThrownBy(() -> client.fetchCurrentWeatherApi(LAT, LON))
			.isInstanceOf(RateLimitExceededException.class);
		assertThatThrownBy(() -> client.fetchForecastApi(LAT, LON))
			.isInstanceOf(RateLimitExceededException.class);
		// 등록한 응답은 한 번만 쓰였다. 두 번째와 세 번째 호출이 나갔다면 여기서 걸린다.
		server.verify();
	}

	private OpenWeatherMapClient clientWith(Bucket bucket) {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		OpenWeatherMapClient client = new OpenWeatherMapClient(restTemplate, new WeatherDataFactory(), bucket);
		ReflectionTestUtils.setField(client, "apiKey", "test-weather-key");
		return client;
	}

	private static Bucket bucketOf(int tokens) {
		return Bucket.builder()
			.addLimit(limit -> limit.capacity(tokens).refillIntervally(tokens, Duration.ofHours(1)))
			.build();
	}
}
