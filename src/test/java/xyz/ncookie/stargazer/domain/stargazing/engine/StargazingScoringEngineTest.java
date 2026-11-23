package xyz.ncookie.stargazer.domain.stargazing.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.shredzone.commons.suncalc.SunPosition;

import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.component.AstronomyCalculator;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.model.StarAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.provider.LightPollutionDataProvider;

/**
 * 점수 엔진의 현재 동작을 그대로 고정하는 특성화 테스트다.
 *
 * 여기 적힌 기대값은 옳다고 판단한 값이 아니라 지금 코드가 내는 값이다. 감점 계수와 경계값은 출처가 없고
 * (docs/SCORING.md "아직 근거가 없는 값") 3단계에서 바뀔 예정이다. 그때 이 테스트가 깨지는 지점이 곧 달라지는 동작이다.
 *
 * 태양 고도는 엔진이 SunPosition을 직접 불러 계산하므로, 실제 천문 계산으로 밤과 낮이 분명한 시각을 골라 쓴다.
 * 달과 광해는 협력 객체를 대역으로 바꿔 값을 직접 정한다.
 */
class StargazingScoringEngineTest {

	// 강릉 안반데기
	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	// 태양 고도 약 -63.8°
	private static final ZonedDateTime NIGHT = ZonedDateTime.of(2026, 1, 15, 23, 0, 0, 0, SEOUL);
	// 태양 고도 약 +30.7°
	private static final ZonedDateTime NOON = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, SEOUL);

	private static final RawAstronomyData MOON_BELOW_HORIZON = astro(-30.0, 1.0);

	private LightPollutionDataProvider lightPollutionDataProvider;
	private AstronomyCalculator astronomyCalculator;
	private StargazingScoringEngine engine;

	@BeforeEach
	void setUp() {
		lightPollutionDataProvider = mock(LightPollutionDataProvider.class);
		astronomyCalculator = mock(AstronomyCalculator.class);
		engine = new StargazingScoringEngine(lightPollutionDataProvider, astronomyCalculator);

		// 기본 조건: 감점 요인이 하나도 없는 밤
		given(1, MOON_BELOW_HORIZON);
	}

	@Nested
	@DisplayName("감점 요인이 없을 때")
	class NoDeduction {

		@Test
		@DisplayName("100점이고 사유는 완벽한 조건이라는 문장 하나다")
		void perfectScore() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(100);
			assertThat(result.reasons()).containsExactly("완벽한 관측 조건입니다!");
		}

		@Test
		@DisplayName("결과에 광해 등급과 천문 데이터를 그대로 싣는다")
		void carriesInputsThrough() {
			RawAstronomyData astro = astro(-30.0, 0.4);
			given(3, astro);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.bortleClass()).isEqualTo(3);
			assertThat(result.astro()).isSameAs(astro);
		}
	}

	@Nested
	@DisplayName("태양 고도")
	class SunAltitude {

		@Test
		@DisplayName("낮에는 0점이고 사유는 해가 떠 있다는 문장 하나다")
		void daytimeIsZero() {
			given(3, MOON_BELOW_HORIZON);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NOON, weather(0, 50, 10000));

			assertThat(result.score()).isZero();
			assertThat(result.reasons()).containsExactly("해가 떠 있어 별이 보이지 않습니다.");
			assertThat(result.bortleClass()).isEqualTo(3);
		}

		@Test
		@DisplayName("낮 판정이 구름 판정보다 먼저다")
		void daytimeCheckedBeforeCloud() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NOON, weather(100, 50, 10000));

			assertThat(result.reasons()).containsExactly("해가 떠 있어 별이 보이지 않습니다.");
		}

		@Test
		@DisplayName("일몰 직후(고도 약 -1.0°)는 낮으로 본다")
		void justAfterSunsetIsDay() {
			ZonedDateTime afterSunset = ZonedDateTime.of(2026, 1, 15, 17, 30, 0, 0, SEOUL);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, afterSunset, weather(0, 50, 10000));

			assertThat(result.score()).isZero();
		}

		@Test
		@DisplayName("시민박명이 끝난 직후(고도 약 -6.4°)는 밤으로 보고 감점 없이 100점을 준다")
		void justAfterCivilTwilightIsNight() {
			ZonedDateTime afterCivilTwilight = ZonedDateTime.of(2026, 1, 15, 18, 0, 0, 0, SEOUL);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, afterCivilTwilight, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(100);
		}

		@Test
		@DisplayName("고도가 정확히 -6.0°이면 밤으로 본다")
		void exactlyMinusSixIsNight() {
			StarAnalysisResult result = scoreWithSunAltitude(-6.0);

			assertThat(result.score()).isEqualTo(100);
		}

		@Test
		@DisplayName("고도가 -6.0°보다 조금이라도 높으면 낮으로 본다")
		void slightlyAboveMinusSixIsDay() {
			StarAnalysisResult result = scoreWithSunAltitude(-5.999);

			assertThat(result.score()).isZero();
			assertThat(result.reasons()).containsExactly("해가 떠 있어 별이 보이지 않습니다.");
		}

		/**
		 * 실제 천문 계산으로는 고도를 정확히 -6.0°에 맞출 수 없어서 SunPosition을 대역으로 바꾼다.
		 * 엔진이 SunPosition.compute().at().on().execute()로 고도를 구한다는 구현에 묶여 있다.
		 */
		private StarAnalysisResult scoreWithSunAltitude(double altitude) {
			SunPosition sunPosition = mock(SunPosition.class);
			when(sunPosition.getAltitude()).thenReturn(altitude);

			SunPosition.Parameters parameters = mock(SunPosition.Parameters.class);
			when(parameters.at(anyDouble(), anyDouble())).thenReturn(parameters);
			when(parameters.on(any(ZonedDateTime.class))).thenReturn(parameters);
			when(parameters.execute()).thenReturn(sunPosition);

			try (MockedStatic<SunPosition> sun = mockStatic(SunPosition.class)) {
				sun.when(SunPosition::compute).thenReturn(parameters);
				return engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));
			}
		}
	}

	@Nested
	@DisplayName("구름")
	class Cloud {

		@ParameterizedTest(name = "구름 {0}%이면 0점")
		@CsvSource({"70", "71", "100"})
		@DisplayName("70% 이상이면 다른 조건과 상관없이 0점이다")
		void cutoff(int cloud) {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(cloud, 50, 10000));

			assertThat(result.score()).isZero();
			assertThat(result.reasons()).containsExactly("구름이 하늘을 덮었습니다 (" + cloud + "%)");
		}

		@Test
		@DisplayName("69%는 0점이 아니라 59점 감점이다. 70%와 1%p 차이로 41점이 갈린다")
		void justBelowCutoff() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(69, 50, 10000));

			assertThat(result.score()).isEqualTo(41);
			assertThat(result.reasons()).containsExactly("구름 69% (-59점)");
		}

		@ParameterizedTest(name = "구름 {0}%이면 {1}점")
		@CsvSource({
			"0, 100",
			"10, 100",
			"11, 99",
			"40, 70",
		})
		@DisplayName("10%까지는 감점이 없고 그 위로는 1%마다 1점을 뺀다")
		void linearDeduction(int cloud, int expectedScore) {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(cloud, 50, 10000));

			assertThat(result.score()).isEqualTo(expectedScore);
		}

		@Test
		@DisplayName("정확히 10%이면 사유에도 구름이 나오지 않는다")
		void tenPercentHasNoReason() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(10, 50, 10000));

			assertThat(result.reasons()).containsExactly("완벽한 관측 조건입니다!");
		}
	}

	@Nested
	@DisplayName("습도")
	class Humidity {

		@ParameterizedTest(name = "습도 {0}%이면 {1}점")
		@CsvSource({
			"0, 100",
			"60, 100",
			"70, 95",
			"100, 80",
		})
		@DisplayName("60%까지는 감점이 없고 그 위로는 2%마다 1점을 뺀다")
		void linearDeduction(double humidity, int expectedScore) {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, humidity, 10000));

			assertThat(result.score()).isEqualTo(expectedScore);
		}

		@Test
		@DisplayName("65%이면 2.5점을 빼고, 점수와 사유의 감점을 각각 반올림해 98점과 -3점이 된다")
		void halfPointRoundsBothWays() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 65, 10000));

			assertThat(result.score()).isEqualTo(98);
			assertThat(result.reasons()).containsExactly("습도 65% (-3점)");
		}

		@Test
		@DisplayName("61%이면 0.5점을 빼고도 반올림으로 100점이 되지만 사유에는 -1점이 남는다")
		void hundredWithDeductionReason() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 61, 10000));

			assertThat(result.score()).isEqualTo(100);
			assertThat(result.reasons()).containsExactly("습도 61% (-1점)");
		}
	}

	@Nested
	@DisplayName("시정")
	class Visibility {

		@ParameterizedTest(name = "시정 {0}m이면 {1}점")
		@CsvSource({
			"20000, 100",
			"10000, 100",
			"9999, 90",
			"5000, 90",
			"4999, 80",
			"0, 80",
		})
		@DisplayName("10km 미만이면 10점, 5km 미만이면 20점을 뺀다")
		void steps(int visibility, int expectedScore) {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, visibility));

			assertThat(result.score()).isEqualTo(expectedScore);
		}

		@Test
		@DisplayName("사유 문장은 단계마다 정해져 있다")
		void reasons() {
			assertThat(engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 9999)).reasons())
				.containsExactly("시야가 다소 흐림 (10km 미만, -10점)");
			assertThat(engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 4999)).reasons())
				.containsExactly("시야가 매우 흐림 (5km 미만, -20점)");
		}

		@Test
		@DisplayName("시정 값이 없으면 10km로 보고 감점하지 않는다")
		void missingVisibility() {
			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, null));

			assertThat(result.score()).isEqualTo(100);
		}
	}

	@Nested
	@DisplayName("달")
	class Moon {

		@ParameterizedTest(name = "빛나는 면적 {0}이면 {1}점")
		@CsvSource({
			"1.0, 70",
			"0.5, 85",
			"0.25, 93",   // 7.5점 감점, 92.5를 반올림
			"0.04, 99",   // 1.2점 감점
		})
		@DisplayName("지평선 위에 있으면 빛나는 면적 비율에 30을 곱해 뺀다")
		void aboveHorizon(double fraction, int expectedScore) {
			given(1, astro(45.0, fraction));

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(expectedScore);
		}

		@Test
		@DisplayName("사유에는 면적 비율과 반올림한 감점을 적는다")
		void reason() {
			given(1, astro(45.0, 0.5));

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.reasons()).containsExactly("달 밝기 50% (-15점)");
		}

		@Test
		@DisplayName("고도는 떠 있는지만 본다. 지평선 바로 위의 보름달도 30점을 뺀다")
		void altitudeOnlyGates() {
			given(1, astro(0.1, 1.0));

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(70);
		}

		@ParameterizedTest(name = "고도 {0}°의 보름달은 감점하지 않는다")
		@CsvSource({"0.0", "-0.1", "-30.0"})
		@DisplayName("고도가 0° 이하이면 보름달이어도 감점하지 않는다")
		void atOrBelowHorizon(double altitude) {
			given(1, astro(altitude, 1.0));

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(100);
			assertThat(result.reasons()).containsExactly("완벽한 관측 조건입니다!");
		}

		@Test
		@DisplayName("감점이 1점에 못 미치면 감점도 사유도 생략한다")
		void belowOnePointIsDropped() {
			given(1, astro(45.0, 0.03)); // 0.9점

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(100);
			assertThat(result.reasons()).containsExactly("완벽한 관측 조건입니다!");
		}
	}

	@Nested
	@DisplayName("광해")
	class LightPollution {

		@ParameterizedTest(name = "Bortle {0}이면 {1}점, 사유에는 -{2}점")
		@CsvSource({
			"2, 94, 6",    // 6.25
			"3, 88, 13",   // 12.5
			"4, 81, 19",   // 18.75
			"5, 75, 25",   // 25
			"6, 69, 31",   // 31.25
			"7, 63, 38",   // 37.5
			"8, 56, 44",   // 43.75
			"9, 50, 50",   // 50
		})
		@DisplayName("등급에서 1을 뺀 값에 6.25를 곱해 뺀다")
		void perClass(int bortle, int expectedScore, int shownDeduction) {
			given(bortle, MOON_BELOW_HORIZON);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(expectedScore);
			assertThat(result.reasons())
				.containsExactly("광해 등급 Class " + bortle + " (-" + shownDeduction + "점)");
		}

		@ParameterizedTest(name = "Bortle {0}은 감점하지 않는다")
		@CsvSource({"1", "0"})
		@DisplayName("1등급은 감점이 없다. 범위를 벗어난 0도 감점 없이 지나간다")
		void noDeduction(int bortle) {
			given(bortle, MOON_BELOW_HORIZON);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 50, 10000));

			assertThat(result.score()).isEqualTo(100);
		}
	}

	@Nested
	@DisplayName("감점 합산")
	class Combined {

		@Test
		@DisplayName("사유는 구름, 습도, 시정, 달, 광해 순서로 쌓인다")
		void reasonOrder() {
			given(5, astro(45.0, 0.5));

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(30, 80, 8000));

			// 100 - 20 - 10 - 10 - 15 - 25
			assertThat(result.score()).isEqualTo(20);
			assertThat(result.reasons()).containsExactly(
				"구름 30% (-20점)",
				"습도 80% (-10점)",
				"시야가 다소 흐림 (10km 미만, -10점)",
				"달 밝기 50% (-15점)",
				"광해 등급 Class 5 (-25점)"
			);
		}

		@Test
		@DisplayName("감점 합이 100을 넘으면 0점으로 막는다. 최대 감점은 179점이다")
		void clampedAtZero() {
			given(9, astro(45.0, 1.0));

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(69, 100, 4999));

			// 100 - 59 - 20 - 20 - 30 - 50 = -79
			assertThat(result.score()).isZero();
			assertThat(result.reasons()).hasSize(5);
		}

		@Test
		@DisplayName("사유에 적힌 감점을 더해도 점수와 맞지 않을 수 있다")
		void shownDeductionsDoNotAddUp() {
			given(4, MOON_BELOW_HORIZON);

			StarAnalysisResult result = engine.calculateScore(LAT, LON, NIGHT, weather(0, 65, 10000));

			// 실제로는 100 - 2.5 - 18.75 = 78.75를 반올림한 79점인데, 사유는 3점과 19점으로 적혀 합이 22점이다.
			assertThat(result.score()).isEqualTo(79);
			assertThat(result.reasons()).containsExactly(
				"습도 65% (-3점)",
				"광해 등급 Class 4 (-19점)"
			);
		}
	}

	private void given(int bortleClass, RawAstronomyData astro) {
		when(lightPollutionDataProvider.getBortleClass(anyDouble(), anyDouble())).thenReturn(bortleClass);
		when(astronomyCalculator.calculate(anyDouble(), anyDouble(), any())).thenReturn(astro);
	}

	private static OpenWeatherResponse weather(int cloud, double humidity, Integer visibility) {
		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(0.0, humidity),
			new OpenWeatherResponse.Clouds(cloud),
			visibility,
			null
		);
	}

	private static RawAstronomyData astro(double moonAltitude, double moonFraction) {
		return new RawAstronomyData(moonFraction, 0.0, moonAltitude, "07:37", "17:30", "05:47", "14:49");
	}
}
