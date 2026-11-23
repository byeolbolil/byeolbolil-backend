package xyz.ncookie.stargazer.domain.stargazing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.ncookie.stargazer.support.LightPollutionFixture.gridPoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.domain.stargazing.repository.LightPollutionRepository;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 분석 API와 예보 API가 지금 내보내는 응답을 고정한다.
 *
 * 장소는 강릉 안반데기, 날씨는 2026-01-15 밤의 3시간 간격 예보(fixtures/openweather/forecast-anbandegi-20260115.json)다.
 * 이 밤은 달이 새벽 5시 47분에 떠서 대부분의 시간에 달 감점이 없다.
 *
 * 요청 시각이 지금과 1시간 넘게 떨어져 있으므로 분석 API는 현재 날씨가 아니라 예보에서 가장 가까운 칸을 쓴다.
 * 현재 날씨를 쓰는 분기는 WeatherDataProviderTest에서 본다.
 */
class StargazingApiTest extends IntegrationTestSupport {

	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;
	private static final String FORECAST = "forecast-anbandegi-20260115.json";

	@Autowired
	private LightPollutionRepository lightPollutionRepository;

	@BeforeEach
	void setUp() {
		lightPollutionRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("GET /api/v1/analyze")
	class Analyze {

		@Test
		@DisplayName("날씨, 천문, 광해를 합친 분석 결과를 공통 응답 형식에 담아 준다")
		void fullResponse() {
			lightPollutionRepository.save(gridPoint(37.625, 128.939, 2));
			externalApi.openWeatherForecastReturns(FORECAST);
			externalApi.geminiReturns("comment-good.json");

			MvcTestResult result = analyze("2026-01-15", "23:00");

			// 23:00에 가장 가까운 칸은 16일 00:00이다. 구름 0%, 습도 50%, 시정 10km라 광해 2등급의 6.25점만 빠진다.
			assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": {
				    "date": "2026-01-15",
				    "time": "23:00",
				    "totalScore": 94,
				    "reasons": ["광해 등급 Class 2 (-6점)"],
				    "aiComment": "하늘이 맑고 어두워서 은하수까지 기대해 볼 만한 밤이에요.",
				    "weather": {
				      "cloudIndex": 0,
				      "humidityIndex": 50,
				      "visibilityText": "매우 좋음"
				    },
				    "astronomy": {
				      "moonPhase": "WANING_CRESCENT",
				      "sunrise": "07:37",
				      "sunset": "17:30",
				      "moonrise": "05:47",
				      "moonset": "14:49"
				    },
				    "lightPollution": {
				      "bortleClass": "Class 2",
				      "brightness": "매우 어두움",
				      "limitingMag": "6.5등급"
				    }
				  }
				}
				""");
		}

		@Test
		@DisplayName("근처에 광해 격자점이 없으면 4등급으로 계산하고, 응답에서 실제 값과 구분되지 않는다")
		void missingLightPollutionBecomesClassFour() {
			externalApi.openWeatherForecastReturns(FORECAST);
			externalApi.geminiReturns("comment-good.json");

			MvcTestResult result = analyze("2026-01-15", "23:00");

			assertThat(result).hasStatusOk().bodyJson()
				.extractingPath("$.data.totalScore").isEqualTo(81);
			assertThat(result).bodyJson()
				.extractingPath("$.data.reasons").asArray().containsExactly("광해 등급 Class 4 (-19점)");
			assertThat(result).bodyJson()
				.extractingPath("$.data.lightPollution.bortleClass").isEqualTo("Class 4");
		}

		@Test
		@DisplayName("Gemini 호출이 실패해도 200이고 점수는 그대로다. 한 줄 평만 대체 문구가 된다")
		void geminiFailureKeepsScore() {
			lightPollutionRepository.save(gridPoint(37.625, 128.939, 2));
			externalApi.openWeatherForecastReturns(FORECAST);
			externalApi.geminiFails();

			MvcTestResult result = analyze("2026-01-15", "23:00");

			assertThat(result).hasStatusOk().bodyJson()
				.extractingPath("$.data.totalScore").isEqualTo(94);
			assertThat(result).bodyJson()
				.extractingPath("$.data.aiComment").isEqualTo("AI 연결 지연");
			// 광해 문구는 Gemini가 아니라 BortleGrade 표에서 오므로 영향이 없다.
			assertThat(result).bodyJson()
				.extractingPath("$.data.lightPollution.brightness").isEqualTo("매우 어두움");
		}

		@Test
		@DisplayName("OpenWeather 예보 호출이 실패하면 400이고, 캐시의 내부 오류 문구가 그대로 나간다")
		void forecastFailureLeaksCacheError() {
			externalApi.openWeatherForecastFails();

			MvcTestResult result = analyze("2026-01-15", "23:00");

			// 클라이언트는 실패를 null로 바꿔 돌려주는데, 예보 캐시가 null을 저장하지 못해 IllegalArgumentException을 던진다.
			// 구름 100%인 대체 날씨로 0점을 주는 경로까지 가지 못한다.
			assertThat(result).hasStatus(400).bodyJson()
				.extractingPath("$.success").isEqualTo(false);
			assertThat(result).bodyJson()
				.extractingPath("$.message").asString()
				.startsWith("Cache 'weatherForecast' does not allow 'null' values");
		}

		@Test
		@DisplayName("예보에 없는 먼 과거 시각을 물어도 가장 가까운 칸의 날씨로 답한다")
		void farPastStillAnswered() {
			lightPollutionRepository.save(gridPoint(37.625, 128.939, 2));
			externalApi.openWeatherForecastReturns(FORECAST);
			externalApi.geminiReturns("comment-good.json");

			// 2020년 1월 15일 23:00의 태양과 달 위치에, 2026년 1월 15일 18:00 칸의 날씨(구름 40%, 습도 70%)를 붙여 계산한다.
			MvcTestResult result = analyze("2020-01-15", "23:00");

			assertThat(result).hasStatusOk().bodyJson()
				.extractingPath("$.data.weather.cloudIndex").isEqualTo(40);
			assertThat(result).bodyJson()
				.extractingPath("$.data.date").isEqualTo("2020-01-15");
		}

		@Test
		@DisplayName("위도가 범위를 벗어나면 400이고 필드별 메시지를 준다")
		void latitudeOutOfRange() {
			MvcTestResult result = mvc.get().uri("/api/v1/analyze")
				.param("lat", "91")
				.param("lon", String.valueOf(LON))
				.param("date", "2026-01-15")
				.param("time", "23:00")
				.exchange();

			assertThat(result).hasStatus(400).bodyJson().isLenientlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "유효성 검사에 실패하였습니다.",
				  "data": { "lat": "위도는 90.0 이하이어야 합니다." }
				}
				""");
		}

		@Test
		@DisplayName("날짜 형식이 틀리면 400이고 형식 오류 메시지를 준다")
		void malformedDate() {
			MvcTestResult result = mvc.get().uri("/api/v1/analyze")
				.param("lat", String.valueOf(LAT))
				.param("lon", String.valueOf(LON))
				.param("date", "2026/01/15")
				.param("time", "23:00")
				.exchange();

			assertThat(result).hasStatus(400).bodyJson()
				.extractingPath("$.data.date").isEqualTo("입력 형식이 올바르지 않습니다.");
		}

		private MvcTestResult analyze(String date, String time) {
			return mvc.get().uri("/api/v1/analyze")
				.param("lat", String.valueOf(LAT))
				.param("lon", String.valueOf(LON))
				.param("date", date)
				.param("time", time)
				.exchange();
		}
	}

	@Nested
	@DisplayName("GET /api/v1/forecast")
	class Forecast {

		@Test
		@DisplayName("밤 시간대 칸만 날짜별로 묶어 준다")
		void groupsNightSlotsByDate() {
			lightPollutionRepository.save(gridPoint(37.625, 128.939, 2));
			externalApi.openWeatherForecastReturns(FORECAST);

			MvcTestResult result = forecast();

			// 16일 09:00 칸은 낮이라 빠진다.
			// 뜨고 지는 시각은 그 날짜의 첫 밤 칸을 기준으로 "그 뒤에 처음 오는" 값이라 두 날짜가 같다.
			assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": {
				    "dailyForecasts": [
				      {
				        "date": "2026-01-15",
				        "sunrise": "07:37",
				        "sunset": "17:30",
				        "moonrise": "05:47",
				        "moonset": "14:49",
				        "hourlyForecasts": [
				          {
				            "time": "18:00",
				            "score": 59,
				            "reasons": ["구름 40% (-30점)", "습도 70% (-5점)", "광해 등급 Class 2 (-6점)"],
				            "starGrade": "4.0등급",
				            "cloudCover": 40,
				            "moonPhase": "WANING_CRESCENT"
				          },
				          {
				            "time": "21:00",
				            "score": 84,
				            "reasons": ["구름 20% (-10점)", "광해 등급 Class 2 (-6점)"],
				            "starGrade": "5.0등급",
				            "cloudCover": 20,
				            "moonPhase": "WANING_CRESCENT"
				          }
				        ]
				      },
				      {
				        "date": "2026-01-16",
				        "sunrise": "07:37",
				        "sunset": "17:30",
				        "moonrise": "05:47",
				        "moonset": "14:49",
				        "hourlyForecasts": [
				          {
				            "time": "00:00",
				            "score": 94,
				            "reasons": ["광해 등급 Class 2 (-6점)"],
				            "starGrade": "6.0등급",
				            "cloudCover": 0,
				            "moonPhase": "WANING_CRESCENT"
				          },
				          {
				            "time": "03:00",
				            "score": 0,
				            "reasons": ["구름이 하늘을 덮었습니다 (80%)"],
				            "starGrade": "2.0등급",
				            "cloudCover": 80,
				            "moonPhase": "WANING_CRESCENT"
				          },
				          {
				            "time": "06:00",
				            "score": 56,
				            "reasons": [
				              "습도 90% (-15점)",
				              "시야가 매우 흐림 (5km 미만, -20점)",
				              "달 밝기 8% (-2점)",
				              "광해 등급 Class 2 (-6점)"
				            ],
				            "starGrade": "6.0등급",
				            "cloudCover": 0,
				            "moonPhase": "WANING_CRESCENT"
				          }
				        ]
				      }
				    ]
				  }
				}
				""");
		}

		@Test
		@DisplayName("같은 좌표를 다시 물으면 OpenWeather를 다시 부르지 않고 캐시로 답한다")
		void secondCallServedFromCache() {
			externalApi.openWeatherForecastReturns(FORECAST);
			assertThat(forecast()).hasStatusOk();

			// 대체 응답을 모두 지운다. 이제 외부 호출이 나가면 테스트가 실패한다.
			externalApi.reset();

			assertThat(forecast()).hasStatusOk().bodyJson()
				.extractingPath("$.data.dailyForecasts").asArray().hasSize(2);
		}

		@Test
		@DisplayName("OpenWeather 예보 호출이 실패하면 빈 목록이 아니라 400이 나간다")
		void forecastFailureLeaksCacheError() {
			externalApi.openWeatherForecastFails();

			MvcTestResult result = forecast();

			// 서비스에는 예보가 null이면 빈 목록을 주는 분기가 있지만, 그 전에 캐시가 예외를 던져 닿지 않는다.
			assertThat(result).hasStatus(400).bodyJson()
				.extractingPath("$.message").asString()
				.startsWith("Cache 'weatherForecast' does not allow 'null' values");
		}

		@Test
		@DisplayName("쓰지 않는 날짜와 시각도 빠지면 400이다")
		void dateAndTimeAreRequired() {
			MvcTestResult result = mvc.get().uri("/api/v1/forecast")
				.param("lat", String.valueOf(LAT))
				.param("lon", String.valueOf(LON))
				.exchange();

			assertThat(result).hasStatus(400).bodyJson().isLenientlyEqualTo("""
				{
				  "data": { "date": "날짜는 필수입니다.", "time": "시간은 필수입니다." }
				}
				""");
		}

		private MvcTestResult forecast() {
			return mvc.get().uri("/api/v1/forecast")
				.param("lat", String.valueOf(LAT))
				.param("lon", String.valueOf(LON))
				.param("date", "2026-01-15")
				.param("time", "23:00")
				.exchange();
		}
	}
}
