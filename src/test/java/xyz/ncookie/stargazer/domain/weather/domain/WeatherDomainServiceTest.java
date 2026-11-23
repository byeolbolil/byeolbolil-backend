package xyz.ncookie.stargazer.domain.weather.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import xyz.ncookie.stargazer.domain.weather.cache.ForecastCache;
import xyz.ncookie.stargazer.domain.weather.client.WeatherProviderException;
import xyz.ncookie.stargazer.domain.weather.client.metnorway.MetNorwayClient;
import xyz.ncookie.stargazer.domain.weather.client.metnorway.MetNorwayForecast;
import xyz.ncookie.stargazer.domain.weather.client.openmeteo.OpenMeteoClient;
import xyz.ncookie.stargazer.domain.weather.exception.WeatherErrorCode;
import xyz.ncookie.stargazer.domain.weather.exception.WeatherException;
import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;
import xyz.ncookie.stargazer.domain.weather.model.ModelRun;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * 캐시, 주 소스, 보조 소스를 어떤 순서와 조건으로 쓰는지 확인한다. 협력 객체는 모두 대역이다.
 */
class WeatherDomainServiceTest {

	// 강릉 안반데기. 칸은 753:2579
	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;
	private static final ForecastCell CELL = ForecastCell.of(LAT, LON);

	private static final Instant NOW = Instant.parse("2026-11-23T18:50:00Z");
	private static final Instant THIS_HOUR = Instant.parse("2026-11-23T18:00:00Z");
	private static final Instant MIDNIGHT = Instant.parse("2026-11-23T00:00:00Z");

	private static final String MODEL = "ecmwf_ifs";
	private static final ModelRun RUN = new ModelRun(Instant.parse("2026-11-23T06:00:00Z"), Duration.ofHours(6));
	private static final String RUN_KEY = "ecmwf_ifs:1795413600:753:2579";
	private static final String UNKNOWN_RUN_KEY = "ecmwf_ifs:unknown-1795456800:753:2579";
	private static final String FALLBACK_KEY = "met-norway:753:2579";

	private OpenMeteoClient openMeteoClient;
	private MetNorwayClient metNorwayClient;
	private ForecastCache forecastCache;
	private WeatherDomainService service;

	@BeforeEach
	void setUp() {
		openMeteoClient = mock(OpenMeteoClient.class);
		metNorwayClient = mock(MetNorwayClient.class);
		forecastCache = mock(ForecastCache.class);
		service = new WeatherDomainService(
			openMeteoClient, metNorwayClient, forecastCache, Clock.fixed(NOW, ZoneOffset.UTC));

		when(openMeteoClient.model()).thenReturn(MODEL);
		// 기본 상태: 캐시는 비어 있고 주 소스는 정상이다.
		when(forecastCache.isPrimaryDown()).thenReturn(false);
		when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.empty());
		when(forecastCache.isModelRunFresh(MODEL)).thenReturn(false);
		when(forecastCache.findForecast(anyString())).thenReturn(Optional.empty());
	}

	@Nested
	@DisplayName("주 소스가 정상일 때")
	class PrimaryHealthy {

		@Test
		@DisplayName("모델 실행 시각을 확인하고, 그 실행과 칸을 키로 예보를 받아 캐시에 넣는다")
		void fetchesAndCaches() {
			WeatherForecast fetched = openMeteoForecast(216);
			when(openMeteoClient.fetchLatestRun()).thenReturn(RUN);
			when(openMeteoClient.fetchForecast(CELL, RUN.initialisedAt())).thenReturn(fetched);

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.OPEN_METEO);
			verify(forecastCache).putModelRun(MODEL, RUN, Duration.ofMinutes(10), Duration.ofHours(12));
			verify(forecastCache).putForecast(RUN_KEY, fetched, Duration.ofHours(12));
			verifyNoInteractions(metNorwayClient);
		}

		@Test
		@DisplayName("지금 시각이 속한 정시부터 168시간만 돌려준다")
		void returnsSevenDaysFromCurrentHour() {
			when(openMeteoClient.fetchLatestRun()).thenReturn(RUN);
			when(openMeteoClient.fetchForecast(CELL, RUN.initialisedAt())).thenReturn(openMeteoForecast(216));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.hours()).hasSize(168);
			assertThat(result.hours().get(0).time()).isEqualTo(THIS_HOUR);
			assertThat(result.hours().get(167).time()).isEqualTo(Instant.parse("2026-11-30T17:00:00Z"));
		}

		@Test
		@DisplayName("같은 실행의 같은 칸이 캐시에 있으면 예보를 다시 받지 않는다")
		void cacheHit() {
			when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.of(RUN));
			when(forecastCache.isModelRunFresh(MODEL)).thenReturn(true);
			when(forecastCache.findForecast(RUN_KEY)).thenReturn(Optional.of(openMeteoForecast(216)));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.hours()).hasSize(168);
			verify(openMeteoClient, never()).fetchLatestRun();
			verify(openMeteoClient, never()).fetchForecast(any(), any());
			verifyNoInteractions(metNorwayClient);
		}

		@Test
		@DisplayName("최근에 확인한 실행 시각이 있으면 다시 조회하지 않는다")
		void freshModelRunIsReused() {
			when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.of(RUN));
			when(forecastCache.isModelRunFresh(MODEL)).thenReturn(true);
			when(openMeteoClient.fetchForecast(CELL, RUN.initialisedAt())).thenReturn(openMeteoForecast(216));

			service.getHourlyForecast(LAT, LON);

			verify(openMeteoClient, never()).fetchLatestRun();
			verify(forecastCache).putForecast(eq(RUN_KEY), any(), eq(Duration.ofHours(12)));
		}

		@Test
		@DisplayName("확인한 지 오래된 실행 시각은 다시 조회하고, 새 실행이면 새 키로 예보를 받는다")
		void staleModelRunIsRefreshed() {
			ModelRun newRun = new ModelRun(Instant.parse("2026-11-23T12:00:00Z"), Duration.ofHours(6));
			when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.of(RUN));
			when(forecastCache.isModelRunFresh(MODEL)).thenReturn(false);
			when(forecastCache.findForecast(RUN_KEY)).thenReturn(Optional.of(openMeteoForecast(216)));
			when(openMeteoClient.fetchLatestRun()).thenReturn(newRun);
			when(openMeteoClient.fetchForecast(CELL, newRun.initialisedAt())).thenReturn(openMeteoForecast(216));

			service.getHourlyForecast(LAT, LON);

			// 2026-11-23T12:00:00Z = 1795435200
			verify(forecastCache).putForecast(eq("ecmwf_ifs:1795435200:753:2579"), any(), eq(Duration.ofHours(12)));
		}
	}

	@Nested
	@DisplayName("모델 실행 시각을 확인하지 못했을 때")
	class ModelRunUnknown {

		@Test
		@DisplayName("마지막으로 확인한 실행 시각이 있으면 그것을 쓴다")
		void usesRememberedRun() {
			when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.of(RUN));
			when(openMeteoClient.fetchLatestRun()).thenThrow(new WeatherProviderException("실패"));
			when(forecastCache.findForecast(RUN_KEY)).thenReturn(Optional.of(openMeteoForecast(216)));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.OPEN_METEO);
			verify(openMeteoClient, never()).fetchForecast(any(), any());
		}

		@Test
		@DisplayName("기억해 둔 값도 없으면 예보는 그대로 받되, 시각 단위 키로 1시간만 캐시한다")
		void fetchesWithHourlyKey() {
			WeatherForecast fetched = openMeteoForecast(216);
			when(openMeteoClient.fetchLatestRun()).thenThrow(new WeatherProviderException("실패"));
			when(openMeteoClient.fetchForecast(CELL, null)).thenReturn(fetched);

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.OPEN_METEO);
			verify(forecastCache).putForecast(UNKNOWN_RUN_KEY, fetched, Duration.ofHours(1));
			verify(forecastCache, never()).markPrimaryDown(any());
		}
	}

	@Nested
	@DisplayName("주 소스가 실패했을 때")
	class PrimaryFails {

		@Test
		@DisplayName("보조 소스에서 받아 돌려주고, 주 소스를 1분 동안 건너뛰도록 표시한다")
		void fallsBack() {
			WeatherForecast fallback = metNorwayForecast(61);
			when(openMeteoClient.fetchLatestRun()).thenReturn(RUN);
			when(openMeteoClient.fetchForecast(CELL, RUN.initialisedAt())).thenThrow(new WeatherProviderException("실패"));
			when(metNorwayClient.fetchForecast(CELL))
				.thenReturn(new MetNorwayForecast(fallback, Instant.parse("2026-11-23T19:18:57Z")));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.MET_NORWAY);
			assertThat(result.hours()).hasSize(61);
			verify(forecastCache).markPrimaryDown(Duration.ofMinutes(1));
			verify(forecastCache).putForecast(FALLBACK_KEY, fallback, Duration.ofMinutes(28).plusSeconds(57));
		}

		@Test
		@DisplayName("건너뛰는 동안에는 주 소스를 부르지 않고 바로 보조 소스로 간다")
		void skipsPrimaryWhileMarkedDown() {
			when(forecastCache.isPrimaryDown()).thenReturn(true);
			when(metNorwayClient.fetchForecast(CELL)).thenReturn(new MetNorwayForecast(metNorwayForecast(61), null));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.MET_NORWAY);
			verify(openMeteoClient, never()).fetchLatestRun();
			verify(openMeteoClient, never()).fetchForecast(any(), any());
		}

		@Test
		@DisplayName("건너뛰는 동안에도 마지막으로 확인한 실행의 예보가 캐시에 있으면 그것을 쓴다")
		void servesCachedPrimaryWhileMarkedDown() {
			when(forecastCache.isPrimaryDown()).thenReturn(true);
			when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.of(RUN));
			when(forecastCache.findForecast(RUN_KEY)).thenReturn(Optional.of(openMeteoForecast(216)));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.OPEN_METEO);
			verifyNoInteractions(metNorwayClient);
		}

		@Test
		@DisplayName("보조 소스 예보가 캐시에 있으면 다시 받지 않는다")
		void fallbackCacheHit() {
			when(forecastCache.isPrimaryDown()).thenReturn(true);
			when(forecastCache.findForecast(FALLBACK_KEY)).thenReturn(Optional.of(metNorwayForecast(61)));

			WeatherForecast result = service.getHourlyForecast(LAT, LON);

			assertThat(result.source()).isEqualTo(ForecastSource.MET_NORWAY);
			verifyNoInteractions(metNorwayClient);
		}

		@ParameterizedTest(name = "Expires {0} → {1}")
		@CsvSource({
			"2026-11-23T19:18:57Z, PT28M57S",   // 남은 시간 그대로
			"2026-11-23T18:50:10Z, PT1M",       // 너무 짧으면 1분
			"2026-11-23T18:00:00Z, PT1M",       // 이미 지났어도 1분
			"2026-11-24T06:00:00Z, PT1H",       // 너무 길면 1시간
		})
		@DisplayName("보조 소스 예보는 Expires까지 캐시하되 1분에서 1시간 사이로 맞춘다")
		void fallbackTtlFollowsExpires(String expires, String expectedTtl) {
			when(forecastCache.isPrimaryDown()).thenReturn(true);
			when(metNorwayClient.fetchForecast(CELL))
				.thenReturn(new MetNorwayForecast(metNorwayForecast(61), Instant.parse(expires)));

			service.getHourlyForecast(LAT, LON);

			verify(forecastCache).putForecast(eq(FALLBACK_KEY), any(), eq(Duration.parse(expectedTtl)));
		}

		@Test
		@DisplayName("Expires가 없으면 30분 동안 캐시한다")
		void fallbackTtlDefault() {
			when(forecastCache.isPrimaryDown()).thenReturn(true);
			when(metNorwayClient.fetchForecast(CELL)).thenReturn(new MetNorwayForecast(metNorwayForecast(61), null));

			service.getHourlyForecast(LAT, LON);

			verify(forecastCache).putForecast(eq(FALLBACK_KEY), any(), eq(Duration.ofMinutes(30)));
		}
	}

	@Nested
	@DisplayName("예보를 줄 수 없을 때")
	class Unavailable {

		@Test
		@DisplayName("두 소스가 모두 실패하면 대체값 없이 예외를 던진다")
		void bothFail() {
			when(openMeteoClient.fetchLatestRun()).thenReturn(RUN);
			when(openMeteoClient.fetchForecast(any(), any())).thenThrow(new WeatherProviderException("실패"));
			when(metNorwayClient.fetchForecast(CELL)).thenThrow(new WeatherProviderException("실패"));

			assertThatThrownBy(() -> service.getHourlyForecast(LAT, LON))
				.isInstanceOf(WeatherException.class)
				.extracting(e -> ((WeatherException) e).getErrorCode())
				.isEqualTo(WeatherErrorCode.FORECAST_UNAVAILABLE);
		}

		@Test
		@DisplayName("받은 예보가 모두 지난 시각이면 예외를 던진다")
		void allHoursInPast() {
			when(forecastCache.findModelRun(MODEL)).thenReturn(Optional.of(RUN));
			when(forecastCache.isModelRunFresh(MODEL)).thenReturn(true);
			when(forecastCache.findForecast(RUN_KEY)).thenReturn(Optional.of(openMeteoForecast(18)));

			assertThatThrownBy(() -> service.getHourlyForecast(LAT, LON)).isInstanceOf(WeatherException.class);
		}
	}

	/** 그날 00:00부터 이어지는 Open-Meteo 예보 */
	private static WeatherForecast openMeteoForecast(int hourCount) {
		return new WeatherForecast(
			ForecastSource.OPEN_METEO, RUN.initialisedAt(), 37.64499, 128.93192, hours(MIDNIGHT, hourCount, 20000));
	}

	/** 지금 시각이 속한 정시부터 이어지는 MET Norway 예보 */
	private static WeatherForecast metNorwayForecast(int hourCount) {
		return new WeatherForecast(
			ForecastSource.MET_NORWAY, Instant.parse("2026-11-23T17:19:38Z"), 37.65, 128.95,
			hours(THIS_HOUR, hourCount, null));
	}

	private static List<HourlyWeather> hours(Instant start, int count, Integer visibility) {
		return IntStream.range(0, count)
			.mapToObj(i -> new HourlyWeather(start.plus(i, ChronoUnit.HOURS), 0, 0, 0, 0, 50, -5.0, 1.0, visibility))
			.toList();
	}
}
