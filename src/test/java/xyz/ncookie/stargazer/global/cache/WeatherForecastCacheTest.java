package xyz.ncookie.stargazer.global.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherMapClient;
import xyz.ncookie.stargazer.domain.stargazing.provider.LightPollutionDataProvider;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 예보와 광해 등급이 Redis에 어떤 키와 만료 시간으로 들어가는지 고정한다.
 * 2단계에서 캐시 키를 모델 격자 셀과 실행 시각으로 바꿀 때 달라지는 지점이다.
 *
 * Redis 쓰기는 비동기라 저장 직후에는 키가 아직 없을 수 있다. 그래서 키가 보일 때까지 잠깐 기다린다.
 */
class WeatherForecastCacheTest extends IntegrationTestSupport {

	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;

	@Autowired
	private OpenWeatherMapClient openWeatherMapClient;

	@Autowired
	private LightPollutionDataProvider lightPollutionDataProvider;

	@Test
	@DisplayName("예보 캐시 키는 소수 넷째 자리로 맞춘 좌표라서, 그 아래 자리만 다른 좌표는 같은 예보를 쓴다")
	void forecastKeyRoundsToFourDecimals() {
		externalApi.openWeatherForecastReturns("forecast-anbandegi-20260115.json");

		openWeatherMapClient.fetchForecastApi(37.62541, 128.93924);

		awaitKey("weatherForecast::37.6254-128.9392");
	}

	@Test
	@DisplayName("예보의 Redis 만료 시간은 10분이다. CacheConfig에 적힌 1시간은 적용되지 않는다")
	void forecastTtlIsTenMinutes() {
		externalApi.openWeatherForecastReturns("forecast-anbandegi-20260115.json");

		openWeatherMapClient.fetchForecastApi(LAT, LON);

		awaitKey("weatherForecast::37.6254-128.9392");
		assertThat(redis.getExpire("weatherForecast::37.6254-128.9392")).isBetween(590L, 600L);
	}

	@Test
	@DisplayName("광해 등급 캐시 키는 좌표를 그대로 이어 붙인 값이고 만료 시간은 10분이다")
	void bortleKeyUsesRawCoordinates() {
		lightPollutionDataProvider.getBortleClass(LAT, LON);
		lightPollutionDataProvider.getBortleClass(37.62541, LON);

		awaitKey("bortleZone::37.6254-128.9392");
		awaitKey("bortleZone::37.62541-128.9392");
		assertThat(redis.getExpire("bortleZone::37.6254-128.9392")).isBetween(590L, 600L);
	}

	private void awaitKey(String key) {
		await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(redis.hasKey(key)));
	}
}
