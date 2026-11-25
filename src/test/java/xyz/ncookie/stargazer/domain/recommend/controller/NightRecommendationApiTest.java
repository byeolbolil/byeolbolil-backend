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
 * 목업의 세 시나리오(좋은 밤, 보름달 밤, 전국 흐림)를 고정 데이터로 재현한다. 로드맵 3단계의 끝나는 조건이다.
 *
 * 장소와 구름 값은 목업(docs/mockup/index.html의 PLACES)에서 가져왔다. 명소 여섯 곳은 data.sql에 있는 실제 명소이고,
 * 내 장소 세 곳은 테스트가 북마크로 넣는다. 그 밖의 명소 58곳은 밤새 흐린 날씨를 받는다.
 *
 * 달은 실제 날짜로 정한다. 2026-11-09는 삭, 2026-11-24는 망이다(USNO, https://aa.usno.navy.mil/data/MoonPhases).
 * 출발지는 목업과 같은 서울 마포다.
 */
class NightRecommendationApiTest extends IntegrationTestSupport {

	private static final LocalDate NEW_MOON_NIGHT = LocalDate.of(2026, 11, 9);
	private static final LocalDate FULL_MOON_NIGHT = LocalDate.of(2026, 11, 24);

	private static final String ORIGIN_LAT = "37.55";
	private static final String ORIGIN_LON = "126.91";

	/** 이름, 위도, 경도, 광해 등급, 저녁 6시 칸부터의 구름 */
	private record Place(String name, double lat, double lon, int bortle, int[] clouds) {}

	private static final List<Place> SPOTS = List.of(
		new Place("화천 조경철천문대", 38.1166, 127.4665, 3, new int[] {5, 5, 10, 10, 15, 10, 20, 25, 35, 45, 55, 60, 70}),
		new Place("함백산 만항재", 37.1481, 128.9142, 3, new int[] {80, 70, 60, 40, 20, 10, 5, 5, 5, 0, 0, 5, 10}),
		new Place("강릉 안반데기", 37.6254, 128.9392, 3, new int[] {20, 15, 10, 25, 60, 80, 80, 70, 40, 20, 10, 10, 15}),
		new Place("영양 반딧불이생태공원", 36.8370, 129.1554, 2, new int[] {10, 5, 5, 0, 0, 5, 10, 10, 20, 40, 60, 70, 70}),
		new Place("양평 중미산천문대", 37.5898, 127.4589, 5, new int[] {40, 30, 20, 20, 25, 40, 60, 70, 80, 80, 90, 90, 90}),
		new Place("영천 보현산천문대", 36.1645, 128.9774, 3, new int[] {90, 90, 90, 85, 80, 85, 90, 90, 95, 95, 90, 90, 90})
	);

	private static final List<Place> MY_PLACES = List.of(
		new Place("호명호수", 37.73, 127.48, 4, new int[] {10, 10, 15, 20, 25, 35, 50, 60, 70, 80, 80, 85, 90}),
		new Place("우리집 옥상", 37.55, 126.91, 8, new int[] {5, 5, 5, 10, 10, 10, 15, 15, 20, 20, 20, 20, 25}),
		new Place("신두리 해안사구", 36.84, 126.20, 4, new int[] {60, 70, 80, 90, 90, 90, 80, 70, 50, 25, 20, 10, 10})
	);

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

	private Member member;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		lightPollutionRepository.deleteAllInBatch();

		member = memberRepository.save(Member.local("stargazer@example.com", "password", "별지기"));
		for (Place place : MY_PLACES) {
			bookmarkRepository.save(Bookmark.builder()
				.member(member)
				.type(BookmarkType.CUSTOM)
				.customName(place.name())
				.latitude(place.lat())
				.longitude(place.lon())
				.address("테스트 주소")
				.build());
		}
		for (Place place : allPlaces()) {
			lightPollutionRepository.save(gridPoint(place.lat(), place.lon(), place.bortle()));
		}
	}

	@Nested
	@DisplayName("좋은 밤: 달이 없고 곳곳이 맑다")
	class GoodNight {

		@BeforeEach
		void given() {
			clock.setTo(Instant.parse("2026-11-09T08:30:00Z")); // 한국 시간 오후 5시 30분
			stubWeather(NEW_MOON_NIGHT, false);
		}

		@Test
		@DisplayName("초저녁부터 일곱 시간 맑은 조경철천문대는 꼭 가볼 만한 곳으로 나온다")
		void strongPlace() {
			MvcTestResult result = tonight(true);

			assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data.date").isEqualTo("2026-11-09");

			Map<String, Object> place = placeNamed(bodyOf(result), "화천 조경철천문대");
			assertThat(place)
				.containsEntry("type", "SPOT")
				.containsEntry("level", "STRONG")
				.containsEntry("pattern", "CLEAR_EARLY")
				.containsEntry("bortleClass", 3)
				.containsEntry("forecastSource", "OPEN_METEO")
				.containsEntry("notes", List.of());
			assertThat(place.get("bestWindow")).isEqualTo(Map.of(
				"start", "2026-11-09T19:00:00+09:00",
				"end", "2026-11-10T02:00:00+09:00",
				"hours", 7));
		}

		@Test
		@DisplayName("꼭 가볼 만한 곳 여섯, 가볼 만한 곳 하나, 잠깐 볼 수 있는 곳 하나 순으로 나온다")
		void order() {
			// 영양은 4시간 반 거리인데도 1시간 44분 거리인 조경철천문대보다 앞에 온다. 하늘이 더 어둡고 구름이 적어서
			// 질 차이가 이동 감점(시간당 4점) 차이보다 크다. 이동 감점이 약하다는 뜻이고, 값은 정책이라 따로 정한다.
			String body = bodyOf(tonight(true));

			List<String> names = JsonPath.read(body, "$.data.places[0:8].name");
			List<String> levels = JsonPath.read(body, "$.data.places[0:8].level");

			assertThat(names).containsExactly(
				"영양 반딧불이생태공원",
				"화천 조경철천문대",
				"함백산 만항재",
				"호명호수",
				"양평 중미산천문대",
				"강릉 안반데기",
				"신두리 해안사구",
				"우리집 옥상"
			);
			assertThat(levels).containsExactly("STRONG", "STRONG", "STRONG", "STRONG", "STRONG", "STRONG", "GOOD", "MAYBE");
		}

		@Test
		@DisplayName("장소마다 밤의 모양이 다르게 나온다")
		void patterns() {
			String body = bodyOf(tonight(true));

			assertThat(placeNamed(body, "함백산 만항재")).containsEntry("pattern", "CLEARS_LATE");
			assertThat(placeNamed(body, "강릉 안반데기")).containsEntry("pattern", "CLEARS_AGAIN");
			assertThat(placeNamed(body, "영양 반딧불이생태공원")).containsEntry("pattern", "CLEAR_EARLY");
			assertThat(placeNamed(body, "우리집 옥상")).containsEntry("pattern", "CLEAR_ALL_NIGHT");
			assertThat(placeNamed(body, "영천 보현산천문대"))
				.containsEntry("pattern", "CLOUDY_ALL_NIGHT")
				.containsEntry("level", "NO")
				.doesNotContainKey("bestWindow");
		}

		@Test
		@DisplayName("새벽에야 개는 곳과 도심에는 주의 사항이 붙는다")
		void notes() {
			String body = bodyOf(tonight(true));

			assertThat(placeNamed(body, "신두리 해안사구")).containsEntry("notes", List.of("CLEARS_LATE"));
			assertThat(placeNamed(body, "우리집 옥상")).containsEntry("notes", List.of("CITY_LIGHTS"));
		}

		@Test
		@DisplayName("출발지를 보내면 직선거리와 이동 시간 추정이 실린다")
		void travel() {
			String body = bodyOf(tonight(true));

			Map<String, Object> home = placeNamed(body, "우리집 옥상");
			assertThat(home.get("travel")).isEqualTo(Map.of("distanceKm", 0.0, "minutes", 0));

			// 서울 마포에서 화천 조경철천문대까지 직선으로 약 80km, 1.3배를 시속 60km로 가면 1시간 44분쯤이다.
			@SuppressWarnings("unchecked")
			Map<String, Object> travel = (Map<String, Object>) placeNamed(body, "화천 조경철천문대").get("travel");
			assertThat((Double) travel.get("distanceKm")).isBetween(79.0, 81.0);
			assertThat((Integer) travel.get("minutes")).isBetween(102, 106);
		}

		@Test
		@DisplayName("밤의 시간 칸마다 구름, 맑음 여부, 점수가 실린다")
		void hours() {
			String body = bodyOf(tonight(true));

			List<Map<String, Object>> hours = JsonPath.read(body, "$.data.places[?(@.name == '화천 조경철천문대')].hours[*]");

			assertThat(hours).hasSize(13);
			assertThat(hours.get(0))
				.containsEntry("time", "2026-11-09T18:00:00+09:00")
				.containsEntry("dark", false)
				.containsEntry("clear", false)
				.containsEntry("score", 0);
			// 구름 5%, 광해 3등급, 달 없음: 100 × 0.95 × 0.875 = 83
			assertThat(hours.get(1))
				.containsEntry("time", "2026-11-09T19:00:00+09:00")
				.containsEntry("dark", true)
				.containsEntry("clear", true)
				.containsEntry("cloudCover", 5)
				.containsEntry("moonUp", false)
				.containsEntry("score", 83);
		}

		@Test
		@DisplayName("로그인하지 않으면 기본 추천 명소만 나온다")
		void anonymous() {
			String body = bodyOf(tonight(false));

			List<String> types = JsonPath.read(body, "$.data.places[*].type");
			int spotCount = (int) observationRepository.count();

			assertThat(types).hasSize(spotCount).containsOnly("SPOT");
			assertThat((Integer) JsonPath.read(body, "$.data.candidateCount")).isEqualTo(spotCount);
			assertThat((String) JsonPath.read(body, "$.data.places[0].name")).isEqualTo("영양 반딧불이생태공원");
		}

		@Test
		@DisplayName("이동 시간 한도를 넘는 곳은 빠지고, 몇 곳이 빠졌는지 알려 준다")
		void maxTravel() {
			MvcTestResult result = mvc.get().uri("/api/v1/recommendations/tonight")
				.header("Authorization", bearer())
				.param("lat", ORIGIN_LAT).param("lon", ORIGIN_LON)
				.param("maxTravelMinutes", "120")
				.exchange();
			String body = bodyOf(result);

			List<String> names = JsonPath.read(body, "$.data.places[*].name");
			List<Integer> minutes = JsonPath.read(body, "$.data.places[*].travel.minutes");

			assertThat(names).contains("화천 조경철천문대", "호명호수").doesNotContain("영양 반딧불이생태공원", "함백산 만항재");
			assertThat(minutes).allSatisfy(m -> assertThat(m).isLessThanOrEqualTo(120));
			assertThat((Integer) JsonPath.read(body, "$.data.excludedByTravelCount")).isPositive();
			assertThat((Integer) JsonPath.read(body, "$.data.criteria.maxTravelMinutes")).isEqualTo(120);
		}

		@Test
		@DisplayName("기준 시간을 5시간으로 올리면 네 시간 맑은 곳은 가볼 만해요로 내려간다")
		void customMinClearHours() {
			MvcTestResult result = mvc.get().uri("/api/v1/recommendations/tonight")
				.header("Authorization", bearer())
				.param("minClearHours", "5")
				.exchange();
			String body = bodyOf(result);

			assertThat(placeNamed(body, "호명호수")).containsEntry("level", "GOOD");
			assertThat(placeNamed(body, "화천 조경철천문대")).containsEntry("level", "STRONG");
			assertThat((Integer) JsonPath.read(body, "$.data.criteria.minClearHours")).isEqualTo(5);
		}

		@Test
		@DisplayName("내 장소로 저장한 명소는 내 장소로 한 번만 나온다")
		void bookmarkedSpotAppearsOnce() {
			ObservationSpot anbandegi = observationRepository.findAll().stream()
				.filter(spot -> spot.getTitle().equals("강릉 안반데기"))
				.findFirst().orElseThrow();
			bookmarkRepository.save(Bookmark.builder().member(member).type(BookmarkType.SPOT).spot(anbandegi).build());

			String body = bodyOf(tonight(true));

			List<Map<String, Object>> matches = JsonPath.read(body, "$.data.places[?(@.name == '강릉 안반데기')]");
			assertThat(matches).hasSize(1);
			assertThat(matches.get(0))
				.containsEntry("type", "BOOKMARK")
				.containsEntry("spotId", anbandegi.getId().intValue());
		}
	}

	@Nested
	@DisplayName("보름달 밤: 같은 구름에 보름달이 밤새 떠 있다")
	class FullMoonNight {

		@BeforeEach
		void given() {
			clock.setTo(Instant.parse("2026-11-24T08:30:00Z"));
			stubWeather(FULL_MOON_NIGHT, false);
		}

		@Test
		@DisplayName("맑은 곳도 한 단계씩 내려가고 달이 밝다는 주의가 붙는다")
		void loweredByMoon() {
			String body = bodyOf(tonight(true));

			assertThat((String) JsonPath.read(body, "$.data.date")).isEqualTo("2026-11-24");
			assertThat(placeNamed(body, "화천 조경철천문대"))
				.containsEntry("level", "GOOD")
				.containsEntry("notes", List.of("BRIGHT_MOON"));
			assertThat(placeNamed(body, "신두리 해안사구"))
				.containsEntry("level", "MAYBE")
				.containsEntry("notes", List.of("CLEARS_LATE", "BRIGHT_MOON"));
			assertThat(placeNamed(body, "우리집 옥상"))
				.containsEntry("level", "MAYBE")
				.containsEntry("notes", List.of("BRIGHT_MOON", "CITY_LIGHTS"));
		}

		@Test
		@DisplayName("꼭 가볼 만한 곳은 하나도 없다")
		void noStrong() {
			String body = bodyOf(tonight(true));

			List<String> levels = JsonPath.read(body, "$.data.places[*].level");

			assertThat(levels).doesNotContain("STRONG");
			assertThat(levels.get(0)).isEqualTo("GOOD");
		}

		@Test
		@DisplayName("달 밝기와 달이 떠 있는지가 응답에 실리고, 시간별 점수도 달만큼 낮아진다")
		void moonInResponse() {
			String body = bodyOf(tonight(true));

			Map<String, Object> place = placeNamed(body, "화천 조경철천문대");
			assertThat((Double) place.get("moonBrightness")).isGreaterThan(0.75);

			// 망의 밤이라 달이 다 차 있고, 저녁에 떠서 다음 날 아침에 진다
			@SuppressWarnings("unchecked")
			Map<String, Object> moon = (Map<String, Object>) place.get("moon");
			assertThat((Double) moon.get("illuminatedFraction")).isGreaterThan(0.99);
			assertThat(moon.get("waxing")).isInstanceOf(Boolean.class);
			assertThat((String) moon.get("rise")).matches("2026-11-24T1[678]:[0-9]{2}:00[+]09:00");
			assertThat((String) moon.get("set")).matches("2026-11-25T0[678]:[0-9]{2}:00[+]09:00");

			List<Map<String, Object>> hours = JsonPath.read(body, "$.data.places[?(@.name == '화천 조경철천문대')].hours[*]");
			assertThat(hours.get(1)).containsEntry("moonUp", true);
			// 달이 없던 밤에는 83점이던 칸이다.
			assertThat((Integer) hours.get(1).get("score")).isBetween(58, 66);
		}
	}

	@Nested
	@DisplayName("전국 흐림: 어디도 맑지 않다")
	class CloudyEverywhere {

		@BeforeEach
		void given() {
			clock.setTo(Instant.parse("2026-11-09T08:30:00Z"));
			stubWeather(NEW_MOON_NIGHT, true);
		}

		@Test
		@DisplayName("모든 장소가 오늘은 쉬어가요이고 추천 시간대가 없다")
		void everyPlaceIsNo() {
			String body = bodyOf(tonight(true));

			List<String> levels = JsonPath.read(body, "$.data.places[*].level");
			List<String> patterns = JsonPath.read(body, "$.data.places[*].pattern");
			List<Object> windows = JsonPath.read(body, "$.data.places[*].bestWindow");

			assertThat(levels).isNotEmpty().containsOnly("NO");
			assertThat(patterns).containsOnly("CLOUDY_ALL_NIGHT");
			assertThat(windows).isEmpty();
		}

		@Test
		@DisplayName("그래도 장소 목록은 가까운 순으로 돌려준다")
		void stillListsPlacesNearestFirst() {
			String body = bodyOf(tonight(true));

			List<Integer> minutes = JsonPath.read(body, "$.data.places[*].travel.minutes");

			assertThat(minutes).isSorted();
			assertThat((String) JsonPath.read(body, "$.data.places[0].name")).isEqualTo("우리집 옥상");
		}
	}

	@Nested
	@DisplayName("날짜와 요청 검증")
	class DatesAndValidation {

		@BeforeEach
		void given() {
			clock.setTo(Instant.parse("2026-11-09T08:30:00Z"));
		}

		@Test
		@DisplayName("날짜를 지정하면 그 날짜에 시작하는 밤을 평가한다")
		void byDate() {
			externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
			externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.uniform(LocalDate.of(2026, 11, 11), 0));

			MvcTestResult result = mvc.get().uri("/api/v1/recommendations/nights/2026-11-11").exchange();
			String body = bodyOf(result);

			assertThat((String) JsonPath.read(body, "$.data.date")).isEqualTo("2026-11-11");
			assertThat((String) JsonPath.read(body, "$.data.places[0].hours[0].time")).startsWith("2026-11-11T18:00");
			assertThat((String) JsonPath.read(body, "$.data.places[0].pattern")).isEqualTo("CLEAR_ALL_NIGHT");
		}

		@Test
		@DisplayName("새벽 4시 전에는 지난 저녁에 시작한 밤이 오늘 밤이다")
		void beforeFourIsStillLastNight() {
			clock.setTo(Instant.parse("2026-11-09T18:30:00Z")); // 한국 시간 10일 새벽 3시 30분
			externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
			externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.uniform(NEW_MOON_NIGHT, 0));

			String body = bodyOf(mvc.get().uri("/api/v1/recommendations/tonight").exchange());

			assertThat((String) JsonPath.read(body, "$.data.date")).isEqualTo("2026-11-09");
			// 이미 지난 시간은 빠지고 새벽 3시 칸부터 남는다.
			assertThat((String) JsonPath.read(body, "$.data.places[0].hours[0].time")).isEqualTo("2026-11-10T03:00:00+09:00");
		}

		@Test
		@DisplayName("지난 날짜나 7일 뒤의 날짜는 400이다")
		void dateOutOfRange() {
			assertThat(mvc.get().uri("/api/v1/recommendations/nights/2026-11-08").exchange())
				.hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("오늘 밤부터 7일 안의 날짜만 볼 수 있습니다.");
			assertThat(mvc.get().uri("/api/v1/recommendations/nights/2026-11-16").exchange()).hasStatus(400);
		}

		@Test
		@DisplayName("날짜 형식이 틀리면 400이다")
		void malformedDate() {
			assertThat(mvc.get().uri("/api/v1/recommendations/nights/tomorrow").exchange())
				.hasStatus(400).bodyJson()
				.extractingPath("$.data.date").isEqualTo("입력 형식이 올바르지 않습니다.");
		}

		@Test
		@DisplayName("출발지는 위도와 경도를 함께 보내야 한다")
		void originNeedsBoth() {
			MvcTestResult result = mvc.get().uri("/api/v1/recommendations/tonight").param("lat", ORIGIN_LAT).exchange();

			assertThat(result).hasStatus(400).bodyJson()
				.extractingPath("$.data.originComplete").isEqualTo("출발지는 위도와 경도를 함께 보내야 합니다.");
		}

		@Test
		@DisplayName("예보를 어디서도 받지 못하면 오류가 아니라 빈 목록을 주고, 평가한 장소가 0곳이라고 알린다")
		void noForecastAnywhere() {
			externalApi.openMeteoModelMetaFails();
			externalApi.openMeteoForecastFails();
			externalApi.metNorwayFails();

			String body = bodyOf(mvc.get().uri("/api/v1/recommendations/tonight").exchange());

			assertThat((Integer) JsonPath.read(body, "$.data.evaluatedCount")).isZero();
			assertThat((Integer) JsonPath.read(body, "$.data.candidateCount")).isPositive();
			assertThat((List<?>) JsonPath.read(body, "$.data.places")).isEmpty();
		}
	}

	// ---- 준비 ----

	private static List<Place> allPlaces() {
		return java.util.stream.Stream.concat(SPOTS.stream(), MY_PLACES.stream()).toList();
	}

	/**
	 * 아홉 곳에는 목업의 구름을, 나머지 명소에는 밤새 흐린 날씨를 준다. cloudyEverywhere면 아홉 곳도 흐리다.
	 */
	private void stubWeather(LocalDate date, boolean cloudyEverywhere) {
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
		if (!cloudyEverywhere) {
			for (Place place : allPlaces()) {
				externalApi.openMeteoForecastReturnsBodyFor(
					place.lat(), place.lon(), OpenMeteoNight.body(date, place.clouds(), 60));
			}
		}
		externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.uniform(date, 95));
	}

	private MvcTestResult tonight(boolean loggedIn) {
		var request = mvc.get().uri("/api/v1/recommendations/tonight")
			.param("lat", ORIGIN_LAT)
			.param("lon", ORIGIN_LON);
		if (loggedIn) {
			request = request.header("Authorization", bearer());
		}
		return request.exchange();
	}

	private String bearer() {
		return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
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
}
