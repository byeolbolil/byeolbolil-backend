package xyz.ncookie.stargazer.domain.stargazing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import xyz.ncookie.stargazer.domain.stargazing.client.gemini.GeminiAnalysisClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.engine.StargazingScoringEngine;
import xyz.ncookie.stargazer.domain.stargazing.model.HourlyForecastData;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.model.StarAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.provider.LightPollutionDataProvider;
import xyz.ncookie.stargazer.domain.stargazing.provider.SkyBrightnessGrid;
import xyz.ncookie.stargazer.domain.stargazing.provider.WeatherDataProvider;

/**
 * 예보 한 칸을 만드는 calculateHourlyForecast의 현재 동작을 고정한다.
 */
class StargazingDomainServiceTest {

	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;
	private static final ZonedDateTime NIGHT = ZonedDateTime.of(2026, 1, 15, 23, 0, 0, 0, ZoneId.of("Asia/Seoul"));

	private final StargazingScoringEngine scoringEngine = mock(StargazingScoringEngine.class);
	private final StargazingDomainService service = new StargazingDomainService(
		mock(WeatherDataProvider.class),
		scoringEngine,
		mock(GeminiAnalysisClient.class),
		mock(LightPollutionDataProvider.class),
		mock(SkyBrightnessGrid.class)
	);

	@ParameterizedTest(name = "구름 {0}% → {1}")
	@CsvSource({
		"0, 6.0등급",
		"10, 5.5등급",
		"20, 5.0등급",
		"50, 3.5등급",
		"100, 1.0등급",
	})
	@DisplayName("예보의 별 등급은 구름량만으로 정한다. 광해 9등급인 도심에서도 구름이 없으면 6.0등급이다")
	void starGradeDependsOnCloudOnly(int cloud, String expectedStarGrade) {
		OpenWeatherResponse weather = weather(cloud);
		engineReturns(weather, new StarAnalysisResult(50, List.of(), 9, astro(0.0)));

		HourlyForecastData forecast = service.calculateHourlyForecast(LAT, LON, NIGHT, weather);

		assertThat(forecast.starGrade()).isEqualTo(expectedStarGrade);
	}

	@Test
	@DisplayName("점수와 사유는 엔진 결과를 그대로 쓰고, 달 위상은 enum 이름으로 싣는다")
	void carriesEngineResult() {
		OpenWeatherResponse weather = weather(20);
		RawAstronomyData astro = astro(-90.0);
		engineReturns(weather, new StarAnalysisResult(84, List.of("구름 20% (-10점)"), 2, astro));

		HourlyForecastData forecast = service.calculateHourlyForecast(LAT, LON, NIGHT, weather);

		assertThat(forecast.dateTime()).isEqualTo(NIGHT);
		assertThat(forecast.score()).isEqualTo(84);
		assertThat(forecast.reasons()).containsExactly("구름 20% (-10점)");
		assertThat(forecast.cloudCover()).isEqualTo(20);
		assertThat(forecast.moonPhase()).isEqualTo("FIRST_QUARTER");
		assertThat(forecast.astro()).isSameAs(astro);
	}

	private void engineReturns(OpenWeatherResponse weather, StarAnalysisResult result) {
		when(scoringEngine.calculateScore(LAT, LON, NIGHT, weather)).thenReturn(result);
	}

	private static OpenWeatherResponse weather(int cloud) {
		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(0.0, 50.0),
			new OpenWeatherResponse.Clouds(cloud),
			10000,
			null
		);
	}

	private static RawAstronomyData astro(double moonPhaseDegree) {
		return new RawAstronomyData(0.5, moonPhaseDegree, -30.0, "07:37", "17:30", "05:47", "14:49");
	}
}
