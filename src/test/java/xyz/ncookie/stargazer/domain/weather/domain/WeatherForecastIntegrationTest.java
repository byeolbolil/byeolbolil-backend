package xyz.ncookie.stargazer.domain.weather.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.ncookie.stargazer.domain.weather.exception.WeatherException;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 2단계의 끝나는 조건을 확인한다. 임의 좌표의 다음 7일 시간별 총운량, 층별 운량, 습도, 이슬점, 시정을 내부 모델로 받는다.
 *
 * 외부 응답은 2026-11-23 18:49(UTC)에 실제로 받아 둔 것이고, 현재 시각을 그 직후로 고정한다.
 * Redis는 컨테이너로 띄운 실제 Redis다.
 */
class WeatherForecastIntegrationTest extends IntegrationTestSupport {

	// 강릉 안반데기. 칸은 753:2579
	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;

	private static final String FORECAST = "forecast-ecmwf-ifs-anbandegi-20261123.json";
	private static final String MODEL_META = "meta-ecmwf-ifs-20261123.json";
	private static final String MET_NORWAY = "complete-anbandegi-20261123.json";
	private static final String MET_NORWAY_EXPIRES = "Mon, 23 Nov 2026 19:18:57 GMT";

	private static final String FORECAST_KEY = "forecast:v1:data:ecmwf_ifs:1795413600:753:2579";
	private static final String FALLBACK_KEY = "forecast:v1:data:met-norway:753:2579";

	@Autowired
	private WeatherDomainService weatherDomainService;

	@BeforeEach
	void fixNow() {
		clock.setTo(Instant.parse("2026-11-23T18:50:00Z"));
	}

	@Test
	@DisplayName("지금 시각이 속한 정시부터 7일, 168시간을 1시간 간격으로 돌려준다")
	void sevenDaysHourly() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastReturns(FORECAST);

		WeatherForecast forecast = weatherDomainService.getHourlyForecast(LAT, LON);

		assertThat(forecast.hours()).hasSize(168);
		assertThat(forecast.hours().get(0).time()).isEqualTo(Instant.parse("2026-11-23T18:00:00Z"));
		assertThat(forecast.hours().get(167).time()).isEqualTo(Instant.parse("2026-11-30T17:00:00Z"));
		for (int i = 1; i < forecast.hours().size(); i++) {
			assertThat(forecast.hours().get(i).time())
				.isEqualTo(forecast.hours().get(i - 1).time().plus(1, ChronoUnit.HOURS));
		}
	}

	@Test
	@DisplayName("시각마다 총운량, 층별 운량, 습도, 이슬점, 기온, 시정이 들어 있다")
	void hourlyValues() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastReturns(FORECAST);

		WeatherForecast forecast = weatherDomainService.getHourlyForecast(LAT, LON);

		assertThat(forecast.hours().get(0)).isEqualTo(new HourlyWeather(
			Instant.parse("2026-11-23T18:00:00Z"), 71, 65, 0, 18, 65, -4.3, 1.5, 34200));
		assertThat(forecast.hours().get(1)).isEqualTo(new HourlyWeather(
			Instant.parse("2026-11-23T19:00:00Z"), 0, 0, 0, 0, 47, -9.5, 0.4, 20080));
		assertThat(forecast.hours()).allSatisfy(hour -> assertThat(hour.visibility()).isNotNull());
	}

	@Test
	@DisplayName("출처, 모델 실행 시각, 예보가 계산된 격자점 좌표를 함께 준다")
	void metadata() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastReturns(FORECAST);

		WeatherForecast forecast = weatherDomainService.getHourlyForecast(LAT, LON);

		assertThat(forecast.source()).isEqualTo(ForecastSource.OPEN_METEO);
		assertThat(forecast.issuedAt()).isEqualTo(Instant.parse("2026-11-23T06:00:00Z"));
		assertThat(forecast.gridLatitude()).isEqualTo(37.64499);
		assertThat(forecast.gridLongitude()).isEqualTo(128.93192);
	}

	@Test
	@DisplayName("모델 실행 시각과 칸을 키로 Redis에 넣고, 갱신 주기의 두 배인 12시간 뒤에 만료시킨다")
	void cachedByModelRunAndCell() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastReturns(FORECAST);

		weatherDomainService.getHourlyForecast(LAT, LON);

		assertThat(redis.hasKey(FORECAST_KEY)).isTrue();
		assertThat(redis.getExpire(FORECAST_KEY)).isBetween(43_100L, 43_200L);
	}

	@Test
	@DisplayName("같은 칸의 다른 좌표를 물으면 외부로 나가지 않고 같은 예보를 준다")
	void sameCellServedFromCache() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastReturns(FORECAST);
		WeatherForecast first = weatherDomainService.getHourlyForecast(LAT, LON);

		// 대체 응답을 모두 지운다. 이제 외부 호출이 나가면 테스트가 실패한다.
		externalApi.reset();
		WeatherForecast second = weatherDomainService.getHourlyForecast(37.66, 128.94);

		assertThat(second).isEqualTo(first);
	}

	@Test
	@DisplayName("Open-Meteo가 실패하면 MET Norway의 1시간 간격 61시간을 시정 없이 준다")
	void fallsBackToMetNorway() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastFails();
		externalApi.metNorwayReturns(MET_NORWAY, MET_NORWAY_EXPIRES);

		WeatherForecast forecast = weatherDomainService.getHourlyForecast(LAT, LON);

		assertThat(forecast.source()).isEqualTo(ForecastSource.MET_NORWAY);
		assertThat(forecast.issuedAt()).isEqualTo(Instant.parse("2026-11-23T17:19:38Z"));
		assertThat(forecast.hours()).hasSize(61);
		assertThat(forecast.hours().get(0)).isEqualTo(new HourlyWeather(
			Instant.parse("2026-11-23T18:00:00Z"), 69, 41, 0, 44, 60, -5.6, 1.6, null));
		assertThat(forecast.hours().get(60).time()).isEqualTo(Instant.parse("2026-11-26T06:00:00Z"));
	}

	@Test
	@DisplayName("MET Norway 응답은 Expires 헤더의 시각까지 캐시한다")
	void fallbackCachedUntilExpires() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastFails();
		externalApi.metNorwayReturns(MET_NORWAY, MET_NORWAY_EXPIRES);

		weatherDomainService.getHourlyForecast(LAT, LON);

		// 18:50:00에서 19:18:57까지 1737초
		assertThat(redis.getExpire(FALLBACK_KEY)).isBetween(1_700L, 1_737L);
	}

	@Test
	@DisplayName("Open-Meteo가 실패한 직후에는 다시 부르지 않고 캐시해 둔 MET Norway 예보를 준다")
	void doesNotRetryPrimaryRightAway() {
		externalApi.openMeteoModelMetaReturns(MODEL_META);
		externalApi.openMeteoForecastFails();
		externalApi.metNorwayReturns(MET_NORWAY, MET_NORWAY_EXPIRES);
		weatherDomainService.getHourlyForecast(LAT, LON);

		externalApi.reset();
		WeatherForecast second = weatherDomainService.getHourlyForecast(LAT, LON);

		assertThat(second.source()).isEqualTo(ForecastSource.MET_NORWAY);
		assertThat(redis.getExpire("forecast:v1:primary-down")).isBetween(50L, 60L);
	}

	@Test
	@DisplayName("두 소스가 모두 실패하면 구름 100% 같은 대체값 없이 예외를 던진다")
	void bothFail() {
		externalApi.openMeteoModelMetaFails();
		externalApi.openMeteoForecastFails();
		externalApi.metNorwayFails();

		assertThatThrownBy(() -> weatherDomainService.getHourlyForecast(LAT, LON))
			.isInstanceOf(WeatherException.class)
			.hasMessage("날씨 예보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.");
	}
}
