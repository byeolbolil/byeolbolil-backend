package xyz.ncookie.stargazer.domain.spot.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 명소 목록은 data.sql이 원본이다. 여기서는 그 데이터가 종류별로 나뉘어 나가는지, 뺀 곳이 감춰지는지를 본다.
 * 곳수는 2026-11-25에 다시 고른 목록의 것이다(docs/decisions.md "명소 목록 재선정").
 */
class ObservationSpotApiTest extends IntegrationTestSupport {

	private static final int OPEN_SITES = 46;
	private static final int FACILITIES = 17;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("조건 없이 부르면 열린 관측지와 체험 시설이 모두 온다")
	void all() {
		String body = bodyOf(mvc.get().uri("/api/v1/spots").exchange());

		List<String> kinds = JsonPath.read(body, "$.data[*].kind");

		assertThat(kinds).hasSize(OPEN_SITES + FACILITIES).containsOnly("OPEN_SITE", "FACILITY");
	}

	@Test
	@DisplayName("kind로 한 종류만 고른다")
	void byKind() {
		String facilities = bodyOf(mvc.get().uri("/api/v1/spots").param("kind", "FACILITY").exchange());
		String openSites = bodyOf(mvc.get().uri("/api/v1/spots").param("kind", "OPEN_SITE").exchange());

		List<String> facilityNames = JsonPath.read(facilities, "$.data[*].title");
		List<String> openNames = JsonPath.read(openSites, "$.data[*].title");

		assertThat(facilityNames).hasSize(FACILITIES).contains("영월 별마로천문대", "양평 중미산천문대");
		assertThat(openNames).hasSize(OPEN_SITES).contains("강릉 안반데기", "합천 오도산").doesNotContain("양평 중미산천문대");
	}

	@Test
	@DisplayName("관측지에는 관측 지점과 소개, 가는 법, 주의 사항이 실린다. 광해 등급은 값이 없어 빠진다")
	void openSiteFields() {
		String body = bodyOf(mvc.get().uri("/api/v1/spots").param("kind", "OPEN_SITE").exchange());

		Map<String, Object> anbandegi = spotNamed(body, "강릉 안반데기");

		assertThat(anbandegi)
			.containsEntry("id", 1)
			.containsEntry("kind", "OPEN_SITE")
			.containsEntry("viewpoint", "멍에전망대 주차장")
			.containsEntry("latitude", 37.62633)
			.containsEntry("longitude", 128.73942)
			.containsEntry("isParkingAvailable", true)
			.doesNotContainKey("bortleScale");
		assertThat((String) anbandegi.get("description")).contains("고랭지 배추밭");
		assertThat((String) anbandegi.get("guide")).contains("멍에전망대 주차장");
		assertThat((String) anbandegi.get("caution")).contains("농로");
	}

	@Test
	@DisplayName("체험 시설에는 관측 지점이 없고 이용 안내가 실린다. 주의 사항은 있는 곳에만 있다")
	void facilityFields() {
		String body = bodyOf(mvc.get().uri("/api/v1/spots").param("kind", "FACILITY").exchange());

		Map<String, Object> byeolmaro = spotNamed(body, "영월 별마로천문대");

		assertThat(byeolmaro)
			.containsEntry("kind", "FACILITY")
			.containsEntry("isCarAccess", false)
			.doesNotContainKeys("viewpoint", "caution");
		assertThat((String) byeolmaro.get("guide")).contains("셔틀버스");
		assertThat(spotNamed(body, "송암스페이스센터")).containsKey("caution");
	}

	@Test
	@DisplayName("자리가 여럿인 명소는 보는 자리를 순서대로 준다. 좌표는 아는 자리에만 있고, 자리가 하나뿐인 곳에는 항목이 없다")
	void points() {
		String body = bodyOf(mvc.get().uri("/api/v1/spots").exchange());

		List<Map<String, Object>> hwangmae = JsonPath.read(body, "$.data[?(@.title == '합천 황매산')].points[*]");

		assertThat(hwangmae).hasSize(2);
		assertThat(hwangmae.get(0)).containsEntry("name", "군립공원 정상 주차장").containsKeys("access", "latitude", "longitude");
		assertThat(hwangmae.get(1)).containsEntry("name", "별빛언덕").doesNotContainKeys("latitude", "longitude");
		assertThat((String) hwangmae.get(1).get("access")).contains("20분");
		assertThat(spotNamed(body, "제천 덕주산성")).doesNotContainKey("points");
	}

	@Test
	@DisplayName("영상을 골라 둔 명소는 유튜브 영상을 순서대로 준다. 여는 주소와 미리보기 그림이 붙고, 영상이 없는 곳에는 항목이 없다")
	void videos() {
		String body = bodyOf(mvc.get().uri("/api/v1/spots").exchange());

		List<Map<String, Object>> hwangmae = JsonPath.read(body, "$.data[?(@.title == '합천 황매산')].videos[*]");

		assertThat(hwangmae).hasSize(4);
		assertThat(hwangmae.get(0))
			.containsEntry("channel", "줌살롱")
			.containsEntry("url", "https://www.youtube.com/watch?v=5HL47ghtf3g")
			.containsEntry("thumbnailUrl", "https://i.ytimg.com/vi/5HL47ghtf3g/mqdefault.jpg")
			.containsEntry("shortForm", false);
		assertThat((String) hwangmae.get(0).get("title")).startsWith("황매산 은하수 추천 포인트 9곳");
		// 작은따옴표가 든 제목도 그대로 온다
		List<String> hwaak = JsonPath.read(body, "$.data[?(@.title == '가평 화악터널 쌈지공원')].videos[*].title");
		assertThat(hwaak).contains("화악터널 쌈지공원, '가평 별빛정원'으로 새단장");
		assertThat(spotNamed(body, "원주 거돈사지")).doesNotContainKey("videos");
		assertThat(spotNamed(body, "대전 시민천문대")).doesNotContainKey("videos");
	}

	@Test
	@DisplayName("참고 링크는 적어 둔 순서대로 오고 미리보기 이미지가 붙는다. 링크가 없는 곳에는 항목이 없다")
	void links() {
		String body = bodyOf(mvc.get().uri("/api/v1/spots").exchange());

		// 차를 대는 자리를 지도에서 특정하지 못한 곳이라 자리를 설명한 글이 첫 번째다
		List<Map<String, Object>> misiryeong = JsonPath.read(body, "$.data[?(@.title == '고성 미시령 옛길')].links[*]");

		assertThat(misiryeong).hasSize(2);
		assertThat(misiryeong.get(0))
			.containsEntry("title", "울산바위 은하수 포인트 가는법 정리")
			.containsEntry("url", "https://gall.dcinside.com/mgallery/board/view/?id=digitalpicture&no=74118");
		assertThat((String) misiryeong.get(1).get("url")).startsWith("https://korean.visitkorea.or.kr/");
		// 미리보기 이미지는 쓸 만한 것이 있는 링크에만 있다. 첫 글의 대표 이미지는 사이트 로고라 비웠다
		assertThat(misiryeong.get(0)).doesNotContainKey("imageUrl");
		assertThat((String) misiryeong.get(1).get("imageUrl")).startsWith("https://cdn.visitkorea.or.kr/");
		assertThat(spotNamed(body, "대전 시민천문대")).containsKey("links");
		assertThat(spotNamed(body, "영월 별마로천문대")).doesNotContainKey("links");
	}

	@Test
	@DisplayName("반경으로 찾으면 가까운 순으로 오고, kind도 함께 걸 수 있다")
	void withinRadius() {
		// 멍에전망대 주차장에서 60km 안
		String body = bodyOf(mvc.get().uri("/api/v1/spots")
			.param("lat", "37.62633").param("lon", "128.73942").param("radius", "60")
			.exchange());
		String facilitiesOnly = bodyOf(mvc.get().uri("/api/v1/spots")
			.param("lat", "37.62633").param("lon", "128.73942").param("radius", "60").param("kind", "FACILITY")
			.exchange());

		List<String> names = JsonPath.read(body, "$.data[*].title");

		assertThat(names.get(0)).isEqualTo("강릉 안반데기");
		assertThat(names).contains("평창 육백마지기", "영월 별마로천문대");
		assertThat((List<String>) JsonPath.read(facilitiesOnly, "$.data[*].kind")).isNotEmpty().containsOnly("FACILITY");
	}

	@Test
	@DisplayName("목록에서 뺀 곳은 전체 조회에도 반경 조회에도 나오지 않는다")
	void retiredIsHidden() {
		jdbcTemplate.update("UPDATE observation_spot SET status = 'RETIRED' WHERE title = '평창 육백마지기'");
		try {
			String all = bodyOf(mvc.get().uri("/api/v1/spots").exchange());
			String near = bodyOf(mvc.get().uri("/api/v1/spots")
				.param("lat", "37.62633").param("lon", "128.73942").param("radius", "60")
				.exchange());

			assertThat((List<String>) JsonPath.read(all, "$.data[*].title"))
				.hasSize(OPEN_SITES + FACILITIES - 1)
				.doesNotContain("평창 육백마지기");
			assertThat((List<String>) JsonPath.read(near, "$.data[*].title")).doesNotContain("평창 육백마지기");
		} finally {
			jdbcTemplate.update("UPDATE observation_spot SET status = 'ACTIVE' WHERE title = '평창 육백마지기'");
		}
	}

	@Test
	@DisplayName("모르는 kind는 400이다")
	void unknownKind() {
		assertThat(mvc.get().uri("/api/v1/spots").param("kind", "CAMPSITE").exchange()).hasStatus(400);
	}

	private static String bodyOf(MvcTestResult result) {
		assertThat(result).hasStatusOk();
		try {
			return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Map<String, Object> spotNamed(String body, String title) {
		List<Map<String, Object>> matches = JsonPath.read(body, "$.data[?(@.title == '" + title + "')]");
		assertThat(matches).as("명소 %s", title).hasSize(1);
		return matches.get(0);
	}
}
