package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import xyz.ncookie.stargazer.domain.recommend.model.ClearWindow;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.NightHour;
import xyz.ncookie.stargazer.domain.recommend.model.NightNote;
import xyz.ncookie.stargazer.domain.recommend.model.NightPattern;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;

/**
 * 해와 달의 위치를 직접 정해 놓고 평가 규칙만 확인한다.
 *
 * 밤의 모양은 목업과 같게 잡았다. 저녁 6시부터 아침 7시까지 열세 칸이 해가 진 시간이고,
 * 그 가운데 저녁 7시부터 새벽 6시까지 열한 칸이 어두운 시간이다. 구름 값은 저녁 6시 칸부터 차례로 적는다.
 * 실제 천문 계산으로 같은 모양이 나오는지는 NightAssessmentEngineRealSkyTest가 본다.
 */
class NightAssessmentEngineTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDate DATE = LocalDate.of(2026, 11, 24);
	private static final double LAT = 37.5;
	private static final double LON = 127.0;
	private static final int DARK_SKY = 3;

	private static final int[] ALL_CLEAR = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	private FakeSky sky;
	private NightAssessmentEngine engine;

	@BeforeEach
	void setUp() {
		sky = new FakeSky();
		engine = new NightAssessmentEngine(sky);
	}

	@Nested
	@DisplayName("어두운 시간")
	class Darkness {

		@Test
		@DisplayName("해가 지평선 아래에 있는 칸만 담고, 그 가운데 처음부터 끝까지 -12° 아래인 칸만 어둡다고 본다")
		void darkHours() {
			NightAssessment night = assess(ALL_CLEAR);

			assertThat(night.hours()).hasSize(13);
			assertThat(night.hours().get(0).time()).isEqualTo(at(DATE, 18));
			assertThat(night.hours().get(12).time()).isEqualTo(at(DATE.plusDays(1), 6));
			assertThat(night.hours()).extracting(NightHour::dark).containsExactly(
				false, true, true, true, true, true, true, true, true, true, true, true, false);
		}

		@Test
		@DisplayName("어둡지 않은 칸은 구름이 없어도 맑은 시간으로 세지 않고 점수도 0이다")
		void twilightIsNeverClear() {
			NightHour dusk = assess(ALL_CLEAR).hours().get(0);

			assertThat(dusk.clear()).isFalse();
			assertThat(dusk.score()).isZero();
		}

		@Test
		@DisplayName("칸의 끝에서만 -12°에 닿아도, 칸 전체가 그 아래이므로 어둡다고 본다")
		void exactlyMinusTwelveAtEdgeIsDark() {
			sky.sunAltitudeAt(19, -12.0);

			NightAssessment night = assess(ALL_CLEAR);

			// 19시 칸은 시작이 정확히 -12°다. 18시 칸은 끝이 -12°지만 시작이 -5°라 어둡지 않다.
			assertThat(night.hours().get(1).dark()).isTrue();
			assertThat(night.hours().get(0).dark()).isFalse();
		}

		@Test
		@DisplayName("예보가 없는 밤은 평가하지 않는다")
		void noForecast() {
			Optional<NightAssessment> night = engine.assess(
				LAT, LON, DATE, SEOUL, DARK_SKY, List.of(), RecommendationPolicy.DEFAULT);

			assertThat(night).isEmpty();
		}

		@Test
		@DisplayName("예보가 밤 중간에 끊기면 예보가 있는 칸까지만 평가한다")
		void partialForecast() {
			List<HourlyWeather> untilMidnight = weather(ALL_CLEAR, 50).subList(0, 6);

			NightAssessment night = engine.assess(
				LAT, LON, DATE, SEOUL, DARK_SKY, untilMidnight, RecommendationPolicy.DEFAULT).orElseThrow();

			assertThat(night.hours()).hasSize(6);
			assertThat(night.bestWindow()).isEqualTo(window(DATE, 19, DATE.plusDays(1), 0));
		}
	}

	@Nested
	@DisplayName("시간별 점수")
	class Score {

		@Test
		@DisplayName("구름도 광해도 달도 없는 완전히 어두운 시간은 100점이다")
		void perfect() {
			assertThat(NightAssessmentEngine.score(0, 1, false, 0.0, -30.0)).isEqualTo(100);
		}

		@ParameterizedTest(name = "구름 {0}% → {1}점")
		@CsvSource({"10, 90", "30, 70", "50, 50", "100, 0"})
		@DisplayName("구름은 가려지지 않은 하늘의 비율만큼 남긴다")
		void cloud(int cloudCover, int expected) {
			assertThat(NightAssessmentEngine.score(cloudCover, 1, false, 0.0, -30.0)).isEqualTo(expected);
		}

		@ParameterizedTest(name = "Bortle {0} → {1}점")
		@CsvSource({"1, 100", "3, 88", "5, 75", "9, 50"})
		@DisplayName("광해는 9등급에서 절반까지 깎는다")
		void lightPollution(int bortleClass, int expected) {
			assertThat(NightAssessmentEngine.score(0, bortleClass, false, 0.0, -30.0)).isEqualTo(expected);
		}

		@ParameterizedTest(name = "달 밝기 {0} → {1}점")
		@CsvSource({"1.0, 70", "0.5, 85", "0.091, 97"})
		@DisplayName("떠 있는 달은 보름달일 때 30%까지 깎는다")
		void moon(double brightness, int expected) {
			assertThat(NightAssessmentEngine.score(0, 1, true, brightness, -30.0)).isEqualTo(expected);
		}

		@Test
		@DisplayName("지평선 아래의 달은 보름달이어도 깎지 않는다")
		void moonBelowHorizon() {
			assertThat(NightAssessmentEngine.score(0, 1, false, 1.0, -30.0)).isEqualTo(100);
		}

		@ParameterizedTest(name = "태양 고도 {0}° → {1}점")
		@CsvSource({"-12, 80", "-15, 90", "-18, 100", "-40, 100"})
		@DisplayName("-12°에서 20%를 깎고 -18°까지 줄여 가며, 그 아래로는 깎지 않는다")
		void twilight(double sunAltitude, int expected) {
			assertThat(NightAssessmentEngine.score(0, 1, false, 0.0, sunAltitude)).isEqualTo(expected);
		}

		@Test
		@DisplayName("요인은 곱으로 묶인다. 광해가 심한 곳에서는 같은 달이 깎는 점수가 줄어든다")
		void factorsMultiply() {
			int darkSiteLoss = NightAssessmentEngine.score(0, 1, false, 0, -30) - NightAssessmentEngine.score(0, 1, true, 1.0, -30);
			int citySiteLoss = NightAssessmentEngine.score(0, 9, false, 0, -30) - NightAssessmentEngine.score(0, 9, true, 1.0, -30);

			assertThat(darkSiteLoss).isEqualTo(30);
			assertThat(citySiteLoss).isEqualTo(15);
		}

		@Test
		@DisplayName("평가에 실리는 점수는 그 칸의 구름, 광해, 달, 태양 고도로 계산한 값이다")
		void appliedPerHour() {
			sky.moonUpFrom(18, 24).moonPhaseAngle(90);

			NightHour hour = assess(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10).hours().get(2);

			// 구름 0.9 × 광해 3등급 0.875 × 반달 (1 - 0.3 × 0.091) = 76.6
			assertThat(hour.score()).isEqualTo(77);
		}
	}

	@Nested
	@DisplayName("맑은 구간")
	class ClearWindows {

		@ParameterizedTest(name = "구름 {0}% → 맑음 {1}")
		@CsvSource({"30, true", "31, false"})
		@DisplayName("구름 30%까지를 맑은 시간으로 센다")
		void clearThreshold(int cloudCover, boolean expectedClear) {
			int[] clouds = new int[13];
			Arrays.fill(clouds, cloudCover);

			assertThat(assess(clouds).hours().get(3).clear()).isEqualTo(expectedClear);
		}

		@Test
		@DisplayName("끊기지 않고 이어지는 맑은 시간을 한 구간으로 묶는다")
		void consecutiveHoursFormOneWindow() {
			// 조경철천문대: 저녁 7시부터 새벽 2시까지 맑고 그 뒤로 흐려진다.
			NightAssessment night = assess(5, 5, 10, 10, 15, 10, 20, 25, 35, 45, 55, 60, 70);

			assertThat(night.clearWindows()).containsExactly(window(DATE, 19, DATE.plusDays(1), 2));
			assertThat(night.bestWindow().hours()).isEqualTo(7);
		}

		@Test
		@DisplayName("중간에 흐려지면 구간이 나뉜다")
		void splitByCloud() {
			// 안반데기: 초저녁에 맑았다가 흐려지고 새벽에 다시 갠다.
			NightAssessment night = assess(20, 15, 10, 25, 60, 80, 80, 70, 40, 20, 10, 10, 15);

			assertThat(night.clearWindows()).containsExactly(
				window(DATE, 19, DATE, 22),
				window(DATE.plusDays(1), 3, DATE.plusDays(1), 6)
			);
		}

		@Test
		@DisplayName("구간이 여럿이면 점수의 합이 큰 쪽을 추천하되, 새벽 2시 뒤의 시간은 절반만 친다")
		void prefersEarlyWindow() {
			// 초저녁 3시간과 새벽 3시간이 똑같이 맑다. 새벽 쪽은 절반으로 쳐서 초저녁이 이긴다.
			NightAssessment night = assess(20, 0, 0, 0, 60, 80, 80, 70, 40, 0, 0, 0, 15);

			assertThat(night.bestWindow()).isEqualTo(window(DATE, 19, DATE, 22));
		}

		@Test
		@DisplayName("새벽 구간이 두 배 넘게 길면 절반으로 쳐도 새벽을 추천한다")
		void longLateWindowWins() {
			// 초저녁 1시간, 새벽 3시간.
			NightAssessment night = assess(20, 0, 60, 60, 60, 80, 80, 70, 40, 0, 0, 0, 15);

			assertThat(night.bestWindow()).isEqualTo(window(DATE.plusDays(1), 3, DATE.plusDays(1), 6));
		}

		@Test
		@DisplayName("추천 구간의 질은 6시간까지만 길이를 쳐준다")
		void qualityCapsAtSixHours() {
			sky.fullyDarkAllNight();

			// 광해 1등급, 구름 0%라 맑은 칸은 모두 100점이다.
			double threeHours = assess(1, clouds(60, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60)).quality();
			double sixHours = assess(1, clouds(60, 0, 0, 0, 0, 0, 0, 60, 60, 60, 60, 60, 60)).quality();
			double sevenHours = assess(1, clouds(60, 0, 0, 0, 0, 0, 0, 0, 60, 60, 60, 60, 60)).quality();

			assertThat(threeHours).isEqualTo(50.0);
			assertThat(sixHours).isEqualTo(100.0);
			assertThat(sevenHours).isEqualTo(100.0);
		}
	}

	@Nested
	@DisplayName("추천 등급")
	class Level {

		@Test
		@DisplayName("새벽 2시 전에 맑은 시간이 3시간 이어지면 꼭 가볼 만해요")
		void strong() {
			NightAssessment night = assess(60, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.STRONG);
			assertThat(night.notes()).isEmpty();
		}

		@Test
		@DisplayName("2시간이면 가볼 만해요")
		void good() {
			NightAssessment night = assess(60, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.GOOD);
		}

		@Test
		@DisplayName("1시간이면 잠깐 볼 수 있어요")
		void maybe() {
			NightAssessment night = assess(60, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.MAYBE);
		}

		@Test
		@DisplayName("맑은 시간이 없으면 오늘은 쉬어가요. 추천 구간도 없다")
		void no() {
			// 보현산천문대: 밤새 흐리다.
			NightAssessment night = assess(90, 90, 90, 85, 80, 85, 90, 90, 95, 95, 90, 90, 90);

			assertThat(night.level()).isEqualTo(RecommendationLevel.NO);
			assertThat(night.bestWindow()).isNull();
			assertThat(night.quality()).isZero();
		}

		@Test
		@DisplayName("3시간이 맑아도 새벽 2시에 걸치면 그 전의 시간만 세어 가볼 만해요가 된다")
		void windowAcrossPreferredTime() {
			// 자정부터 새벽 4시까지 맑다. 새벽 2시 전은 2시간이다.
			NightAssessment night = assess(60, 60, 60, 60, 60, 60, 0, 0, 0, 0, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.GOOD);
			assertThat(night.notes()).doesNotContain(NightNote.CLEARS_LATE);
		}

		@Test
		@DisplayName("추천 구간이 모두 새벽 2시 뒤면 늦게 맑아진다는 주의를 붙인다")
		void clearsLateNote() {
			// 신두리 해안사구: 새벽 3시부터 맑아진다.
			NightAssessment night = assess(60, 70, 80, 90, 90, 90, 80, 70, 50, 25, 20, 10, 10);

			assertThat(night.level()).isEqualTo(RecommendationLevel.GOOD);
			assertThat(night.notes()).containsExactly(NightNote.CLEARS_LATE);
		}

		@Test
		@DisplayName("설정에서 기준 시간을 2시간으로 낮추면 2시간으로도 꼭 가볼 만해요가 된다")
		void customMinClearHours() {
			NightAssessment night = assess(DARK_SKY, clouds(60, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60),
				new RecommendationPolicy(2, LocalTime.of(2, 0)));

			assertThat(night.level()).isEqualTo(RecommendationLevel.STRONG);
		}

		@Test
		@DisplayName("설정에서 선호 시각을 밤 10시로 당기면 그 전의 시간만 센다")
		void customPreferBefore() {
			// 저녁 7시부터 자정까지 5시간 맑다. 밤 10시 전은 3시간이다.
			NightAssessment evening = assess(DARK_SKY, clouds(60, 0, 0, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60),
				new RecommendationPolicy(4, LocalTime.of(22, 0)));

			assertThat(evening.level()).isEqualTo(RecommendationLevel.GOOD);
		}
	}

	@Nested
	@DisplayName("달")
	class Moon {

		@Test
		@DisplayName("밝은 달이 추천 구간의 절반 이상 떠 있으면 등급을 한 단계 내리고 주의를 붙인다")
		void brightMoonLowersLevel() {
			sky.moonUpFrom(18, 24).moonPhaseAngle(0);

			NightAssessment night = assess(60, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.GOOD);
			assertThat(night.notes()).containsExactly(NightNote.BRIGHT_MOON);
		}

		@Test
		@DisplayName("달이 추천 구간의 절반보다 짧게 떠 있으면 내리지 않는다")
		void moonUpLessThanHalf() {
			// 추천 구간은 저녁 7시부터 밤 10시까지 3시간, 달은 저녁 8시에 진다.
			sky.moonUpFrom(18, 20).moonPhaseAngle(0);

			NightAssessment night = assess(60, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.STRONG);
			assertThat(night.notes()).isEmpty();
		}

		@ParameterizedTest(name = "위상각 {0}° → {1}")
		@CsvSource({
			"56, GOOD",     // 밝기 약 0.252
			"57, STRONG",   // 밝기 약 0.246
		})
		@DisplayName("보름 대비 밝기 0.25가 경계다")
		void brightnessThreshold(double phaseAngle, RecommendationLevel expected) {
			sky.moonUpFrom(18, 24).moonPhaseAngle(phaseAngle);

			NightAssessment night = assess(60, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(expected);
		}

		@Test
		@DisplayName("잠깐 볼 수 있어요는 달이 밝아도 더 내리지 않는다")
		void doesNotLowerBelowMaybe() {
			sky.moonUpFrom(18, 24).moonPhaseAngle(0);

			NightAssessment night = assess(60, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60);

			assertThat(night.level()).isEqualTo(RecommendationLevel.MAYBE);
			assertThat(night.notes()).containsExactly(NightNote.BRIGHT_MOON);
		}
	}

	@Nested
	@DisplayName("광해와 습도")
	class LightPollutionAndHumidity {

		@Test
		@DisplayName("Bortle 7등급부터는 밤새 맑아도 잠깐 볼 수 있어요까지만 준다")
		void cityLights() {
			// 우리집 옥상(서울): 밤새 맑지만 도시 한가운데다.
			NightAssessment night = assess(8, clouds(5, 5, 5, 10, 10, 10, 15, 15, 20, 20, 20, 20, 25));

			assertThat(night.level()).isEqualTo(RecommendationLevel.MAYBE);
			assertThat(night.notes()).containsExactly(NightNote.CITY_LIGHTS);
		}

		@ParameterizedTest(name = "Bortle {0} → {1}")
		@CsvSource({"6, STRONG", "7, MAYBE"})
		@DisplayName("6등급까지는 낮추지 않는다")
		void cityThreshold(int bortleClass, RecommendationLevel expected) {
			assertThat(assess(bortleClass, ALL_CLEAR).level()).isEqualTo(expected);
		}

		@ParameterizedTest(name = "습도 {0}% → 이슬 주의 {1}")
		@CsvSource({"80, true", "79, false"})
		@DisplayName("추천 구간의 습도가 80% 이상이면 이슬 주의를 붙인다")
		void dew(int humidity, boolean expectedNote) {
			NightAssessment night = engine.assess(
				LAT, LON, DATE, SEOUL, DARK_SKY, weather(ALL_CLEAR, humidity), RecommendationPolicy.DEFAULT).orElseThrow();

			assertThat(night.notes().contains(NightNote.DEW)).isEqualTo(expectedNote);
			assertThat(night.level()).isEqualTo(RecommendationLevel.STRONG);
		}
	}

	@Nested
	@DisplayName("밤 패턴")
	class Pattern {

		@Test
		@DisplayName("어두운 시간의 90% 이상이 맑으면 밤새 맑아요")
		void clearAllNight() {
			// 열한 칸 가운데 열 칸이 맑다.
			assertThat(assess(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 60, 0).pattern()).isEqualTo(NightPattern.CLEAR_ALL_NIGHT);
		}

		@Test
		@DisplayName("어두워지자마자 맑고 나중에 흐려지면 초저녁이 가장 맑아요")
		void clearEarly() {
			assertThat(assess(5, 5, 10, 10, 15, 10, 20, 25, 35, 45, 55, 60, 70).pattern())
				.isEqualTo(NightPattern.CLEAR_EARLY);
		}

		@Test
		@DisplayName("흐리다가 맑아져 날이 밝을 때까지 맑으면 늦은 밤에 개요")
		void clearsLate() {
			// 만항재: 밤 10시부터 갠다.
			assertThat(assess(80, 70, 60, 40, 20, 10, 5, 5, 5, 0, 0, 5, 10).pattern())
				.isEqualTo(NightPattern.CLEARS_LATE);
		}

		@Test
		@DisplayName("맑은 구간이 둘 이상이면 흐렸다가 다시 개요")
		void clearsAgain() {
			assertThat(assess(20, 15, 10, 25, 60, 80, 80, 70, 40, 20, 10, 10, 15).pattern())
				.isEqualTo(NightPattern.CLEARS_AGAIN);
		}

		@Test
		@DisplayName("밤 한가운데에만 맑으면 잠깐 맑은 틈이 있어요")
		void briefClear() {
			assertThat(assess(60, 60, 60, 60, 0, 0, 60, 60, 60, 60, 60, 60, 60).pattern())
				.isEqualTo(NightPattern.BRIEF_CLEAR);
		}

		@Test
		@DisplayName("맑은 시간이 없으면 밤새 흐려요")
		void cloudyAllNight() {
			assertThat(assess(90, 90, 90, 85, 80, 85, 90, 90, 95, 95, 90, 90, 90).pattern())
				.isEqualTo(NightPattern.CLOUDY_ALL_NIGHT);
		}
	}

	// ---- 준비 ----

	private NightAssessment assess(int... clouds) {
		return assess(DARK_SKY, clouds);
	}

	private NightAssessment assess(int bortleClass, int[] clouds) {
		return assess(bortleClass, clouds, RecommendationPolicy.DEFAULT);
	}

	private NightAssessment assess(int bortleClass, int[] clouds, RecommendationPolicy policy) {
		return engine.assess(LAT, LON, DATE, SEOUL, bortleClass, weather(clouds, 50), policy).orElseThrow();
	}

	private static int[] clouds(int... values) {
		return values;
	}

	/** 저녁 6시 칸부터 한 시간씩의 예보 */
	private static List<HourlyWeather> weather(int[] clouds, int humidity) {
		List<HourlyWeather> hours = new ArrayList<>();
		for (int i = 0; i < clouds.length; i++) {
			Instant time = at(DATE, 18).plusSeconds(3600L * i);
			hours.add(new HourlyWeather(time, clouds[i], clouds[i], 0, 0, humidity, 0.0, 5.0, 20000));
		}
		return hours;
	}

	private static Instant at(LocalDate date, int hour) {
		return ZonedDateTime.of(date, LocalTime.of(hour, 0), SEOUL).toInstant();
	}

	private static ClearWindow window(LocalDate startDate, int startHour, LocalDate endDate, int endHour) {
		return new ClearWindow(at(startDate, startHour), at(endDate, endHour));
	}

	/**
	 * 정해 둔 해와 달의 위치를 돌려주는 대역.
	 *
	 * 기본값은 해가 저녁 6시에 -5°, 7시에 -13°, 그 뒤로 -30°였다가 아침 6시에 -13°, 7시에 -5°가 되는 밤이다.
	 * 그래서 저녁 7시 칸부터 새벽 5시 칸까지가 어둡다. 달은 기본으로 떠 있지 않다.
	 */
	private static class FakeSky extends SkyEphemeris {

		private final double[] sunByHour = new double[24];
		private int moonUpFromHour = -1;
		private int moonUpUntilHour = -1;
		private double moonPhaseAngle = 180.0;

		FakeSky() {
			Arrays.fill(sunByHour, 10.0);
			for (int hour : new int[] {20, 21, 22, 23, 0, 1, 2, 3, 4, 5}) {
				sunByHour[hour] = -30.0;
			}
			sunByHour[18] = -5.0;
			sunByHour[19] = -13.0;
			sunByHour[6] = -13.0;
			sunByHour[7] = -5.0;
		}

		void sunAltitudeAt(int hour, double altitude) {
			sunByHour[hour] = altitude;
		}

		/** 박명 감점이 섞이지 않게, 어두워지는 순간부터 완전히 어두운 밤으로 만든다. */
		void fullyDarkAllNight() {
			sunByHour[19] = -30.0;
			sunByHour[6] = -30.0;
		}

		/** 저녁 fromHour시부터 untilHour시(24면 자정) 전까지 달이 떠 있다. */
		FakeSky moonUpFrom(int fromHour, int untilHour) {
			this.moonUpFromHour = fromHour;
			this.moonUpUntilHour = untilHour;
			return this;
		}

		FakeSky moonPhaseAngle(double angle) {
			this.moonPhaseAngle = angle;
			return this;
		}

		@Override
		public double sunAltitude(double latitude, double longitude, Instant time) {
			return sunByHour[time.atZone(SEOUL).getHour()];
		}

		@Override
		public double moonAltitude(double latitude, double longitude, Instant time) {
			int hour = time.atZone(SEOUL).getHour();
			return (hour >= moonUpFromHour && hour < moonUpUntilHour) ? 30.0 : -30.0;
		}

		@Override
		public double moonPhaseAngle(Instant time) {
			return moonPhaseAngle;
		}
	}
}
