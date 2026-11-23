package xyz.ncookie.stargazer.domain.stargazing.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherMapClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.mapper.OpenWeatherResponseMapper;
import xyz.ncookie.stargazer.global.util.WeatherDataFactory;

/**
 * 요청 시각에 따라 어떤 날씨 데이터를 고르는지 고정한다.
 * 현재 시각과 비교하는 분기가 있어서 요청 시각은 지금을 기준으로 만든다.
 */
class WeatherDataProviderTest {

	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final OpenWeatherMapClient client = mock(OpenWeatherMapClient.class);
	private final WeatherDataProvider provider = new WeatherDataProvider(
		client,
		new OpenWeatherResponseMapper(),
		new WeatherDataFactory()
	);

	@Nested
	@DisplayName("현재 날씨와 예보 가운데 고르기")
	class CurrentOrForecast {

		@Test
		@DisplayName("요청 시각이 지금부터 1시간 안쪽이면 현재 날씨를 쓴다")
		void withinAnHourUsesCurrent() {
			OpenWeatherResponse current = weather(15);
			when(client.fetchCurrentWeatherApi(LAT, LON)).thenReturn(current);

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, now().plusMinutes(30));

			assertThat(result).isSameAs(current);
			verify(client, never()).fetchForecastApi(LAT, LON);
		}

		@Test
		@DisplayName("1시간 안쪽이면 이미 지난 시각이어도 현재 날씨를 쓴다")
		void recentPastUsesCurrent() {
			OpenWeatherResponse current = weather(15);
			when(client.fetchCurrentWeatherApi(LAT, LON)).thenReturn(current);

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, now().minusMinutes(30));

			assertThat(result).isSameAs(current);
		}

		@Test
		@DisplayName("1시간 넘게 떨어져 있으면 예보를 쓴다")
		void beyondAnHourUsesForecast() {
			ZonedDateTime target = now().plusHours(5);
			when(client.fetchForecastApi(LAT, LON)).thenReturn(forecast(item(target, 25)));

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, target);

			assertThat(result.clouds().all()).isEqualTo(25);
			verify(client, never()).fetchCurrentWeatherApi(LAT, LON);
		}
	}

	@Nested
	@DisplayName("예보에서 한 칸 고르기")
	class NearestForecastItem {

		@Test
		@DisplayName("요청 시각에 가장 가까운 칸을 고른다")
		void picksNearest() {
			ZonedDateTime target = now().plusHours(10);
			when(client.fetchForecastApi(LAT, LON)).thenReturn(forecast(
				item(target.minusMinutes(100), 10),
				item(target.plusMinutes(80), 20),
				item(target.plusMinutes(260), 30)
			));

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, target);

			assertThat(result.clouds().all()).isEqualTo(20);
		}

		@Test
		@DisplayName("앞뒤 칸이 같은 거리면 먼저 나온 칸을 고른다")
		void tieGoesToEarlierItem() {
			ZonedDateTime target = now().plusHours(10);
			when(client.fetchForecastApi(LAT, LON)).thenReturn(forecast(
				item(target.minusMinutes(90), 10),
				item(target.plusMinutes(90), 20)
			));

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, target);

			assertThat(result.clouds().all()).isEqualTo(10);
		}

		@Test
		@DisplayName("거리에 상한이 없다. 예보 범위를 한참 벗어난 과거 시각에도 가장 가까운 칸으로 답한다")
		void noUpperBoundOnDistance() {
			ZonedDateTime farPast = ZonedDateTime.of(2020, 1, 1, 22, 0, 0, 0, SEOUL);
			when(client.fetchForecastApi(LAT, LON)).thenReturn(forecast(
				item(now().plusHours(3), 10),
				item(now().plusHours(6), 20)
			));

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, farPast);

			assertThat(result.clouds().all()).isEqualTo(10);
		}

		@Test
		@DisplayName("예보 칸에 시정이 없으면 10km로 채운다")
		void missingVisibilityBecomesTenKm() {
			ZonedDateTime target = now().plusHours(5);
			OpenWeatherForecastResponse.Item withoutVisibility = new OpenWeatherForecastResponse.Item(
				target.toEpochSecond(),
				new OpenWeatherForecastResponse.Main(0.0, 50.0),
				null,
				new OpenWeatherForecastResponse.Clouds(0),
				null,
				null
			);
			when(client.fetchForecastApi(LAT, LON)).thenReturn(forecast(withoutVisibility));

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, target);

			assertThat(result.visibility()).isEqualTo(10000);
		}
	}

	@Nested
	@DisplayName("예보를 받지 못했을 때")
	class ForecastUnavailable {

		@Test
		@DisplayName("예보가 null이면 구름 100%인 대체 날씨를 돌려준다")
		void nullForecastBecomesOvercast() {
			when(client.fetchForecastApi(LAT, LON)).thenReturn(null);

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, now().plusHours(5));

			assertThat(result.clouds().all()).isEqualTo(100);
			assertThat(result.main().humidity()).isEqualTo(50.0);
			assertThat(result.visibility()).isEqualTo(10000);
		}

		@Test
		@DisplayName("예보 목록이 비어 있어도 구름 100%인 대체 날씨를 돌려준다")
		void emptyForecastBecomesOvercast() {
			when(client.fetchForecastApi(LAT, LON)).thenReturn(forecast());

			OpenWeatherResponse result = provider.fetchWeatherData(LAT, LON, now().plusHours(5));

			assertThat(result.clouds().all()).isEqualTo(100);
		}
	}

	private static ZonedDateTime now() {
		return ZonedDateTime.now(SEOUL);
	}

	private static OpenWeatherResponse weather(int cloud) {
		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(0.0, 50.0),
			new OpenWeatherResponse.Clouds(cloud),
			10000,
			null
		);
	}

	private static OpenWeatherForecastResponse forecast(OpenWeatherForecastResponse.Item... items) {
		return new OpenWeatherForecastResponse(List.of(items));
	}

	private static OpenWeatherForecastResponse.Item item(ZonedDateTime time, int cloud) {
		return new OpenWeatherForecastResponse.Item(
			time.toEpochSecond(),
			new OpenWeatherForecastResponse.Main(0.0, 50.0),
			null,
			new OpenWeatherForecastResponse.Clouds(cloud),
			10000,
			null
		);
	}
}
