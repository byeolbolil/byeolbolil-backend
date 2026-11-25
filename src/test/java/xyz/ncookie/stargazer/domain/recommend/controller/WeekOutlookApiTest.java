package xyz.ncookie.stargazer.domain.recommend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.ncookie.stargazer.support.LightPollutionFixture.gridPoint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;
import xyz.ncookie.stargazer.domain.spot.repository.ObservationRepository;
import xyz.ncookie.stargazer.domain.stargazing.repository.LightPollutionRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;
import xyz.ncookie.stargazer.support.OpenMeteoNight;

/**
 * 한 장소의 이번 주 밤. 밤마다 구름을 다르게 준 일주일 예보로 날짜별 평가가 나오는지 본다.
 *
 * 첫 밤은 2026-11-09이고 삭이다(USNO, https://aa.usno.navy.mil/data/MoonPhases). 그 뒤로 달이 차오르지만
 * 15일에도 초저녁에 지는 초승달이라 등급을 낮추지 않는다. 등급 값은 지금의 평가 결과를 고정한 것이다.
 */
class WeekOutlookApiTest extends IntegrationTestSupport {

	private static final LocalDate FIRST_NIGHT = LocalDate.of(2026, 11, 9);

	// 강릉 안반데기 멍에전망대 주차장
	private static final String LAT = "37.62633";
	private static final String LON = "128.73942";

	/** 밤마다의 총운량: 맑음, 흐림, 맑음, 구름 조금, 흐림, 맑음, 맑음 */
	private static final int[] WEEK_CLOUDS = {0, 95, 0, 40, 95, 0, 0};

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private ObservationRepository observationRepository;

	@Autowired
	private LightPollutionRepository lightPollutionRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		lightPollutionRepository.deleteAllInBatch();
		lightPollutionRepository.save(gridPoint(37.62633, 128.73942, 3));
		lightPollutionRepository.save(gridPoint(37.55, 126.91, 8));

		clock.setTo(Instant.parse("2026-11-09T08:30:00Z")); // 한국 시간 오후 5시 30분
	}

	@Nested
	@DisplayName("좌표로 고른 한 위치")
	class AtLocation {

		@Test
		@DisplayName("오늘 밤부터 일곱 밤을 날짜순으로 평가한다")
		void sevenNights() {
			stubWeek(WEEK_CLOUDS);

			String body = bodyOf(week(LAT, LON));

			assertThat((String) JsonPath.read(body, "$.data.startDate")).isEqualTo("2026-11-09");
			assertThat((List<String>) JsonPath.read(body, "$.data.dates")).containsExactly(
				"2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-14", "2026-11-15");
			assertThat((List<?>) JsonPath.read(body, "$.data.places")).hasSize(1);
			assertThat((List<String>) JsonPath.read(body, "$.data.places[0].nights[*].date")).containsExactly(
				"2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-14", "2026-11-15");
			assertThat((List<String>) JsonPath.read(body, "$.data.places[0].nights[*].level")).containsExactly(
				"STRONG", "NO", "STRONG", "NO", "NO", "STRONG", "STRONG");
		}

		@Test
		@DisplayName("좌표로 물은 위치에는 이름과 종류가 없고, 광해 등급과 예보 출처가 실린다")
		void placeFields() {
			stubWeek(WEEK_CLOUDS);

			Map<String, Object> place = JsonPath.read(bodyOf(week(LAT, LON)), "$.data.places[0]");

			assertThat(place)
				.containsEntry("latitude", 37.62633)
				.containsEntry("longitude", 128.73942)
				.containsEntry("bortleClass", 3)
				.containsEntry("forecastSource", "OPEN_METEO")
				.doesNotContainKeys("type", "id", "spotId", "name", "address");
		}

		@Test
		@DisplayName("맑은 밤에는 추천 시간대와 시간별 조건이 있고, 흐린 밤에는 추천 시간대가 없다")
		void nightFields() {
			stubWeek(WEEK_CLOUDS);

			String body = bodyOf(week(LAT, LON));
			Map<String, Object> clear = JsonPath.read(body, "$.data.places[0].nights[0]");
			Map<String, Object> cloudy = JsonPath.read(body, "$.data.places[0].nights[1]");

			assertThat(clear)
				.containsEntry("pattern", "CLEAR_ALL_NIGHT")
				.containsEntry("notes", List.of())
				.containsKeys("moon", "moonBrightness");
			assertThat(clear.get("bestWindow")).isEqualTo(Map.of(
				"start", "2026-11-09T19:00:00+09:00",
				"end", "2026-11-10T06:00:00+09:00",
				"hours", 11));
			assertThat((String) JsonPath.read(body, "$.data.places[0].nights[0].hours[0].time"))
				.isEqualTo("2026-11-09T18:00:00+09:00");

			assertThat(cloudy)
				.containsEntry("pattern", "CLOUDY_ALL_NIGHT")
				.doesNotContainKey("bestWindow");
			assertThat((List<?>) cloudy.get("hours")).isNotEmpty();
		}

		@Test
		@DisplayName("기준을 보내면 그 기준으로 평가하고 응답에 적는다")
		void customCriteria() {
			stubWeek(WEEK_CLOUDS);

			String body = bodyOf(mvc.get().uri("/api/v1/recommendations/week")
				.param("lat", LAT).param("lon", LON)
				.param("minClearHours", "8").param("preferBefore", "23:00")
				.exchange());

			assertThat((Integer) JsonPath.read(body, "$.data.criteria.minClearHours")).isEqualTo(8);
			assertThat((String) JsonPath.read(body, "$.data.criteria.preferBefore")).isEqualTo("23:00");
			// 밤 11시 전에 여덟 시간 맑을 수는 없어서 맑은 밤이 한 단계 내려간다
			assertThat((String) JsonPath.read(body, "$.data.places[0].nights[0].level")).isEqualTo("GOOD");
		}

		@Test
		@DisplayName("예보가 사흘 치뿐이면 사흘 밤만 온다")
		void shortForecast() {
			stubWeek(new int[] {0, 0, 0});

			String body = bodyOf(week(LAT, LON));

			assertThat((List<String>) JsonPath.read(body, "$.data.places[0].nights[*].date"))
				.containsExactly("2026-11-09", "2026-11-10", "2026-11-11");
			assertThat((List<?>) JsonPath.read(body, "$.data.dates")).hasSize(7);
		}

		@Test
		@DisplayName("새벽 4시 전에는 지난 저녁에 시작한 밤부터 센다")
		void beforeFourStartsLastNight() {
			clock.setTo(Instant.parse("2026-11-09T18:30:00Z")); // 한국 시간 10일 새벽 3시 30분
			stubWeek(WEEK_CLOUDS);

			String body = bodyOf(week(LAT, LON));

			assertThat((String) JsonPath.read(body, "$.data.startDate")).isEqualTo("2026-11-09");
			assertThat((String) JsonPath.read(body, "$.data.places[0].nights[0].hours[0].time"))
				.isEqualTo("2026-11-10T03:00:00+09:00");
		}

		@Test
		@DisplayName("좌표를 보내지 않으면 400이다")
		void locationRequired() {
			assertThat(mvc.get().uri("/api/v1/recommendations/week").exchange())
				.hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("볼 위치의 위도와 경도를 보내야 합니다.");
			assertThat(mvc.get().uri("/api/v1/recommendations/week").param("lat", LAT).exchange())
				.hasStatus(400).bodyJson()
				.extractingPath("$.data.locationComplete").isEqualTo("위치는 위도와 경도를 함께 보내야 합니다.");
		}

		@Test
		@DisplayName("예보를 받지 못하면 503이다")
		void forecastUnavailable() {
			externalApi.openMeteoModelMetaFails();
			externalApi.openMeteoForecastFails();
			externalApi.metNorwayFails();

			assertThat(week(LAT, LON)).hasStatus(503);
		}
	}

	@Nested
	@DisplayName("내 장소")
	class MyPlaces {

		private Member member;

		@BeforeEach
		void given() {
			member = memberRepository.save(Member.local("stargazer@example.com", "password", "별지기"));
			ObservationSpot anbandegi = observationRepository.findAll().stream()
				.filter(spot -> spot.getTitle().equals("강릉 안반데기"))
				.findFirst().orElseThrow();
			bookmarkRepository.save(Bookmark.builder()
				.member(member)
				.type(BookmarkType.CUSTOM)
				.customName("우리집 옥상")
				.latitude(37.55)
				.longitude(126.91)
				.address("서울 마포구")
				.build());
			bookmarkRepository.save(Bookmark.builder()
				.member(member)
				.type(BookmarkType.SPOT)
				.spot(anbandegi)
				.build());
		}

		@Test
		@DisplayName("저장한 장소마다 일곱 밤이 온다. 도심의 옥상은 맑아도 등급이 낮다")
		void everySavedPlace() {
			stubWeek(WEEK_CLOUDS);

			String body = bodyOf(myWeek(true));

			assertThat((List<String>) JsonPath.read(body, "$.data.places[*].name"))
				.containsExactlyInAnyOrder("우리집 옥상", "강릉 안반데기");
			assertThat((List<String>) JsonPath.read(body, "$.data.places[*].type")).containsOnly("BOOKMARK");

			Map<String, Object> spot = placeNamed(body, "강릉 안반데기");
			assertThat(spot).containsEntry("bortleClass", 3).containsKeys("id", "spotId", "address");
			assertThat(levelsOf(body, "강릉 안반데기"))
				.containsExactly("STRONG", "NO", "STRONG", "NO", "NO", "STRONG", "STRONG");

			assertThat(placeNamed(body, "우리집 옥상")).containsEntry("bortleClass", 8).doesNotContainKey("spotId");
			assertThat(levelsOf(body, "우리집 옥상"))
				.containsExactly("MAYBE", "NO", "MAYBE", "NO", "NO", "MAYBE", "MAYBE");
		}

		@Test
		@DisplayName("로그인하지 않았으면 장소가 없는 응답이 온다")
		void anonymous() {
			String body = bodyOf(myWeek(false));

			assertThat((List<?>) JsonPath.read(body, "$.data.places")).isEmpty();
			assertThat((List<?>) JsonPath.read(body, "$.data.dates")).hasSize(7);
		}

		@Test
		@DisplayName("예보를 받지 못한 장소는 사라지지 않고 밤이 없는 채로 온다")
		void forecastUnavailable() {
			externalApi.openMeteoModelMetaFails();
			externalApi.openMeteoForecastFails();
			externalApi.metNorwayFails();

			String body = bodyOf(myWeek(true));

			assertThat((List<?>) JsonPath.read(body, "$.data.places")).hasSize(2);
			assertThat((List<?>) JsonPath.read(body, "$.data.places[0].nights")).isEmpty();
			assertThat((Map<String, Object>) JsonPath.read(body, "$.data.places[0]")).doesNotContainKey("forecastSource");
		}

		private MvcTestResult myWeek(boolean loggedIn) {
			var request = mvc.get().uri("/api/v1/recommendations/week/mine");
			if (loggedIn) {
				request = request.header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()));
			}
			return request.exchange();
		}
	}

	// ---- 준비 ----

	private void stubWeek(int[] nightClouds) {
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
		externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.nights(FIRST_NIGHT, nightClouds));
	}

	private MvcTestResult week(String lat, String lon) {
		return mvc.get().uri("/api/v1/recommendations/week").param("lat", lat).param("lon", lon).exchange();
	}

	private static String bodyOf(MvcTestResult result) {
		assertThat(result).hasStatusOk();
		try {
			return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Map<String, Object> placeNamed(String body, String name) {
		List<Map<String, Object>> matches = JsonPath.read(body, "$.data.places[?(@.name == '" + name + "')]");
		assertThat(matches).as("장소 %s", name).hasSize(1);
		return matches.get(0);
	}

	private static List<String> levelsOf(String body, String name) {
		return JsonPath.read(body, "$.data.places[?(@.name == '" + name + "')].nights[*].level");
	}
}
