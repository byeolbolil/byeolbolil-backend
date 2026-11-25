package xyz.ncookie.stargazer.domain.recommend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.ncookie.stargazer.support.LightPollutionFixture.gridPoint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.domain.stargazing.repository.LightPollutionRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;
import xyz.ncookie.stargazer.support.OpenMeteoNight;

/**
 * 밤마다 가장 좋은 등급. 장소마다 다른 일주일 예보를 주고, 밤마다 가장 높은 등급이 오는지와 그 등급이 날짜별
 * 밤 추천의 첫 장소와 같은지 본다.
 *
 * 첫 밤은 2026-11-09이고 삭이다(USNO, https://aa.usno.navy.mil/data/MoonPhases). 그 주에는 달이 등급을 낮추지
 * 않는다. 등급 값은 지금의 평가 결과를 고정한 것이다.
 */
class NightSummaryApiTest extends IntegrationTestSupport {

	private static final LocalDate FIRST_NIGHT = LocalDate.of(2026, 11, 9);

	// 서울 마포. 내 장소인 옥상의 자리이자 출발지다
	private static final double HOME_LAT = 37.55;
	private static final double HOME_LON = 126.91;

	// 강릉 안반데기 멍에전망대 주차장. 마포에서 3시간 반쯤 걸린다
	private static final double ANBANDEGI_LAT = 37.62633;
	private static final double ANBANDEGI_LON = 128.73942;

	/** 안반데기의 밤마다 총운량: 맑음, 흐림, 맑음, 구름 조금, 흐림, 맑음, 맑음 */
	private static final int[] ANBANDEGI_CLOUDS = {0, 95, 0, 40, 95, 0, 0};

	/** 옥상은 둘째 밤에만 맑다 */
	private static final int[] HOME_CLOUDS = {95, 0, 95, 95, 95, 95, 95};

	/** 나머지 관측지는 일주일 내내 흐리다 */
	private static final int[] CLOUDY_WEEK = {95, 95, 95, 95, 95, 95, 95};

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

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
		lightPollutionRepository.save(gridPoint(ANBANDEGI_LAT, ANBANDEGI_LON, 3));
		lightPollutionRepository.save(gridPoint(HOME_LAT, HOME_LON, 8));

		member = memberRepository.save(Member.local("stargazer@example.com", "password", "별지기"));
		bookmarkRepository.save(Bookmark.builder()
			.member(member)
			.type(BookmarkType.CUSTOM)
			.customName("우리집 옥상")
			.latitude(HOME_LAT)
			.longitude(HOME_LON)
			.address("서울 마포구")
			.build());

		clock.setTo(Instant.parse("2026-11-09T08:30:00Z")); // 한국 시간 오후 5시 30분
	}

	@Test
	@DisplayName("오늘 밤부터 일곱 밤마다 후보 가운데 가장 높은 등급이 온다")
	void bestLevelOfEachNight() {
		stubWeek();

		String body = bodyOf(nights(false));

		assertThat((String) JsonPath.read(body, "$.data.startDate")).isEqualTo("2026-11-09");
		assertThat((List<String>) JsonPath.read(body, "$.data.nights[*].date")).containsExactly(
			"2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-14", "2026-11-15");
		// 안반데기만 맑은 밤이 있다. 구름이 40%인 넷째 밤은 맑은 시간이 없어 어려운 밤이다
		assertThat((List<String>) JsonPath.read(body, "$.data.nights[*].level")).containsExactly(
			"STRONG", "NO", "STRONG", "NO", "NO", "STRONG", "STRONG");
		assertThat((Map<String, Object>) JsonPath.read(body, "$.data.criteria"))
			.containsEntry("minClearHours", 3)
			.containsEntry("preferBefore", "02:00")
			.containsEntry("maxTravelMinutes", null);
	}

	@Test
	@DisplayName("로그인했으면 내 장소도 후보에 넣는다. 옥상만 맑은 밤은 옥상의 등급이 된다")
	void includesMyPlaces() {
		stubWeek();

		String body = bodyOf(nights(true));

		assertThat((List<String>) JsonPath.read(body, "$.data.nights[*].level")).containsExactly(
			"STRONG", "MAYBE", "STRONG", "NO", "NO", "STRONG", "STRONG");
	}

	@Test
	@DisplayName("밤마다의 등급은 같은 조건으로 부른 날짜별 밤 추천의 첫 장소 등급과 같다")
	void sameAsFirstPlaceOfEachNight() {
		stubWeek();

		List<Map<String, Object>> nights = JsonPath.read(bodyOf(nights(true)), "$.data.nights");

		assertThat(nights).hasSize(7);
		for (Map<String, Object> night : nights) {
			String detail = bodyOf(mvc.get().uri("/api/v1/recommendations/nights/" + night.get("date"))
				.header("Authorization", bearer())
				.exchange());
			assertThat((String) JsonPath.read(detail, "$.data.places[0].level"))
				.as("%s 밤", night.get("date"))
				.isEqualTo(night.get("level"));
		}
	}

	@Test
	@DisplayName("이동 시간 한도를 넘는 곳은 보지 않는다. 안반데기가 빠지면 옥상만 맑은 밤만 남는다")
	void maxTravel() {
		stubWeek();

		String body = bodyOf(mvc.get().uri("/api/v1/recommendations/nights")
			.header("Authorization", bearer())
			.param("lat", String.valueOf(HOME_LAT)).param("lon", String.valueOf(HOME_LON))
			.param("maxTravelMinutes", "120")
			.exchange());

		assertThat((List<String>) JsonPath.read(body, "$.data.nights[*].level")).containsExactly(
			"NO", "MAYBE", "NO", "NO", "NO", "NO", "NO");
		assertThat((Integer) JsonPath.read(body, "$.data.criteria.maxTravelMinutes")).isEqualTo(120);
	}

	@Test
	@DisplayName("한도 안에 후보가 하나도 없으면 밤은 일곱 개가 오되 등급이 빠진다")
	void nothingWithinReach() {
		stubWeek();

		// 로그인하지 않아 옥상이 없고, 마포에서 0분 거리인 관측지는 없다
		String body = bodyOf(mvc.get().uri("/api/v1/recommendations/nights")
			.param("lat", String.valueOf(HOME_LAT)).param("lon", String.valueOf(HOME_LON))
			.param("maxTravelMinutes", "0")
			.exchange());

		List<Map<String, Object>> nights = JsonPath.read(body, "$.data.nights");
		assertThat(nights).hasSize(7).allSatisfy(night -> assertThat(night).containsOnlyKeys("date"));
	}

	@Test
	@DisplayName("기준을 보내면 그 기준으로 평가한다")
	void customCriteria() {
		stubWeek();

		String body = bodyOf(mvc.get().uri("/api/v1/recommendations/nights")
			.param("minClearHours", "8").param("preferBefore", "23:00")
			.exchange());

		// 밤 11시 전에 여덟 시간 맑을 수는 없어서 맑은 밤이 한 단계 내려간다
		assertThat((List<String>) JsonPath.read(body, "$.data.nights[*].level")).containsExactly(
			"GOOD", "NO", "GOOD", "NO", "NO", "GOOD", "GOOD");
		assertThat((Integer) JsonPath.read(body, "$.data.criteria.minClearHours")).isEqualTo(8);
	}

	@Test
	@DisplayName("예보가 사흘 치뿐이면 넷째 밤부터는 등급이 빠진다")
	void shortForecast() {
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
		externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.nights(FIRST_NIGHT, 0, 0, 0));

		List<Map<String, Object>> nights = JsonPath.read(bodyOf(nights(false)), "$.data.nights");

		assertThat(nights).hasSize(7);
		assertThat(nights.subList(0, 3)).allSatisfy(night -> assertThat(night).containsEntry("level", "STRONG"));
		assertThat(nights.subList(3, 7)).allSatisfy(night -> assertThat(night).containsOnlyKeys("date"));
	}

	@Test
	@DisplayName("새벽 4시 전에는 지난 저녁에 시작한 밤부터 센다")
	void beforeFourStartsLastNight() {
		clock.setTo(Instant.parse("2026-11-09T18:30:00Z")); // 한국 시간 10일 새벽 3시 30분
		stubWeek();

		String body = bodyOf(nights(false));

		assertThat((String) JsonPath.read(body, "$.data.startDate")).isEqualTo("2026-11-09");
		assertThat((String) JsonPath.read(body, "$.data.nights[0].date")).isEqualTo("2026-11-09");
	}

	@Test
	@DisplayName("예보를 어디서도 받지 못하면 오류가 아니라 등급이 없는 일곱 밤이 온다")
	void noForecastAnywhere() {
		externalApi.openMeteoModelMetaFails();
		externalApi.openMeteoForecastFails();
		externalApi.metNorwayFails();

		List<Map<String, Object>> nights = JsonPath.read(bodyOf(nights(false)), "$.data.nights");

		assertThat(nights).hasSize(7).allSatisfy(night -> assertThat(night).containsOnlyKeys("date"));
	}

	@Test
	@DisplayName("출발지는 위도와 경도를 함께 보내야 한다")
	void originNeedsBoth() {
		MvcTestResult result = mvc.get().uri("/api/v1/recommendations/nights")
			.param("lat", String.valueOf(HOME_LAT))
			.exchange();

		assertThat(result).hasStatus(400).bodyJson()
			.extractingPath("$.data.originComplete").isEqualTo("출발지는 위도와 경도를 함께 보내야 합니다.");
	}

	// ---- 준비 ----

	/** 안반데기와 옥상에는 저마다의 일주일을, 나머지 관측지에는 내내 흐린 일주일을 준다 */
	private void stubWeek() {
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
		externalApi.openMeteoForecastReturnsBodyFor(
			ANBANDEGI_LAT, ANBANDEGI_LON, OpenMeteoNight.nights(FIRST_NIGHT, ANBANDEGI_CLOUDS));
		externalApi.openMeteoForecastReturnsBodyFor(HOME_LAT, HOME_LON, OpenMeteoNight.nights(FIRST_NIGHT, HOME_CLOUDS));
		externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.nights(FIRST_NIGHT, CLOUDY_WEEK));
	}

	private MvcTestResult nights(boolean loggedIn) {
		var request = mvc.get().uri("/api/v1/recommendations/nights");
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
}
