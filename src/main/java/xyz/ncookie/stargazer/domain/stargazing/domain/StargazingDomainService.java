package xyz.ncookie.stargazer.domain.stargazing.domain;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.client.gemini.GeminiAnalysisClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.engine.StargazingScoringEngine;
import xyz.ncookie.stargazer.domain.stargazing.enums.MoonPhase;
import xyz.ncookie.stargazer.domain.stargazing.model.HourlyForecastData;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.provider.LightPollutionDataProvider;
import xyz.ncookie.stargazer.domain.stargazing.provider.SkyBrightnessGrid;
import xyz.ncookie.stargazer.domain.stargazing.provider.WeatherDataProvider;
import xyz.ncookie.stargazer.domain.stargazing.model.GeminiAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.model.SkyLight;
import xyz.ncookie.stargazer.domain.stargazing.model.StarAnalysisResult;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StargazingDomainService {

	private final WeatherDataProvider weatherDataProvider;
	private final StargazingScoringEngine scoringEngine;
	private final GeminiAnalysisClient geminiAnalysisClient;
	private final LightPollutionDataProvider lightPollutionDataProvider;
	private final SkyBrightnessGrid skyBrightnessGrid;

	/**
	 * 좌표의 광해 등급(Bortle 1~9). 근처에 광해 데이터가 없으면 4를 돌려준다.
	 * 이전 분석 API만 쓴다. 밤 추천은 findSkyLight의 값을 쓴다.
	 */
	public int getBortleClass(double lat, double lon) {
		return lightPollutionDataProvider.getBortleClass(lat, lon);
	}

	/**
	 * 좌표의 하늘 밝기와 둘레의 불빛. 자료가 있는 범위(한국 둘레) 밖이면 비어 있다.
	 */
	public Optional<SkyLight> findSkyLight(double lat, double lon) {
		return skyBrightnessGrid.lookup(lat, lon);
	}

	public OpenWeatherResponse fetchWeatherData(double lat, double lon, ZonedDateTime targetDateTime) {
		return weatherDataProvider.fetchWeatherData(lat, lon, targetDateTime);
	}

	public StarAnalysisResult calculateScore(double lat, double lon, ZonedDateTime targetDateTime, OpenWeatherResponse weatherData) {
		return scoringEngine.calculateScore(lat, lon, targetDateTime, weatherData);
	}

	public HourlyForecastData calculateHourlyForecast(
		double lat,
		double lon,
		ZonedDateTime dateTime,
		OpenWeatherResponse weatherData
	) {

		StarAnalysisResult result = calculateScore(lat, lon, dateTime, weatherData);

		String starGrade = String.format("%.1f등급", 6.0 - (weatherData.clouds().all() / 20.0));

		return new HourlyForecastData(
			dateTime,
			result.score(),
			result.reasons(),
			starGrade,
			weatherData.clouds().all(),
			MoonPhase.calculate(result.astro().moonPhaseDegree()).name(),
			result.astro()
		);
	}

	public GeminiAnalysisResult getAnalysis(
		int score,
		List<String> reasons,
		double lat,
		double lon,
		OpenWeatherResponse weatherData,
		RawAstronomyData astro,
		int bortleClass
	) {
		return geminiAnalysisClient.getAnalysis(score, reasons, lat, lon, weatherData, astro, bortleClass);
	}
}
