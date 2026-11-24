package xyz.ncookie.stargazer.domain.geocode.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 지명 검색 API의 응답을 고정한다. Photon의 응답은 2026-11-25에 받아 둔 것이다(fixtures/photon).
 */
class GeocodeApiTest extends IntegrationTestSupport {

	@Nested
	@DisplayName("GET /api/v1/geocode/search")
	class Search {

		@Test
		@DisplayName("로그인 없이 이름, 주소, 좌표를 준다. 산이 도로보다 앞에 온다")
		void search() {
			externalApi.photonSearchReturns("search-hwaaksan-20261125.json");

			assertThat(search("화악산")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": [
				    { "name": "화악산", "address": "경기도 가평군 북면", "latitude": 37.9946674, "longitude": 127.5034314 },
				    { "name": "화악산", "address": "경상북도 청도군 청도읍", "latitude": 35.5932802, "longitude": 128.6915855 },
				    { "name": "화악산 응봉", "address": "강원특별자치도 춘천시", "latitude": 38.0000278, "longitude": 127.5405556 },
				    { "name": "화악산 북봉", "address": "경기도 가평군 북면", "latitude": 37.9977519, "longitude": 127.5041268 },
				    { "name": "화악산 중봉", "address": "경기도 가평군 북면", "latitude": 37.9896066, "longitude": 127.4984159 },
				    { "name": "화악산 상봉", "address": "경기도 가평군 북면", "latitude": 37.99478, "longitude": 127.50333 },
				    { "name": "화악산농장자연휴양림", "address": "강원특별자치도 화천군 사내면 광덕리",
				      "latitude": 38.0655299, "longitude": 127.4850585 },
				    { "name": "화악산 등산로", "address": "경상북도 청도군 청도읍", "latitude": 35.5992601, "longitude": 128.692627 }
				  ]
				}
				""");
		}

		@Test
		@DisplayName("이름이 같은 역보다 군과 읍이 앞에 온다")
		void areasBeforeStations() {
			externalApi.photonSearchReturns("search-yangpyeong-20261125.json");

			assertThat(search("양평")).hasStatusOk().bodyJson()
				.extractingPath("$.data[*].name").asArray()
				.containsExactly("양평군", "양평 용문사 은행나무", "양평읍", "양평동", "양평2동", "양평1동", "양평 현대 2차", "양평리");
		}

		@Test
		@DisplayName("찾은 곳이 없으면 빈 배열이다")
		void noResult() {
			externalApi.photonSearchReturns("search-no-result-20261125.json");

			assertThat(search("육백마지기")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": []
				}
				""");
		}

		@Test
		@DisplayName("같은 검색어를 다시 보내면 Photon을 부르지 않는다. 앞뒤 공백은 같은 검색어다")
		void cached() {
			externalApi.photonSearchReturns("search-hwaaksan-20261125.json");
			search("화악산");
			// 이 뒤로는 어떤 외부 호출도 테스트를 실패시킨다
			externalApi.reset();

			assertThat(search(" 화악산 ")).hasStatusOk().bodyJson()
				.extractingPath("$.data[0].name").isEqualTo("화악산");
		}

		@Test
		@DisplayName("q가 빠지면 400이다")
		void missingQuery() {
			assertThat(mvc.get().uri("/api/v1/geocode/search").exchange())
				.hasStatus(400).bodyJson().isStrictlyEqualTo("""
					{
					  "success": false,
					  "status": 400,
					  "message": "유효성 검사에 실패하였습니다.",
					  "data": { "q": "검색어를 입력해 주세요." }
					}
					""");
		}

		@Test
		@DisplayName("한 글자는 400이다")
		void tooShort() {
			assertThat(search("강")).hasStatus(400).bodyJson()
				.extractingPath("$.data.q").isEqualTo("검색어는 2자 이상 50자 이하여야 합니다.");
		}

		@Test
		@DisplayName("Photon이 답하지 않으면 503이다")
		void providerDown() {
			externalApi.photonSearchFails();

			assertThat(search("화악산")).hasStatus(503).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 503,
				  "message": "지명 검색을 지금 쓸 수 없습니다. 잠시 후 다시 시도해 주세요."
				}
				""");
		}

		private MvcTestResult search(String query) {
			return mvc.get().uri("/api/v1/geocode/search").param("q", query).exchange();
		}
	}

	@Nested
	@DisplayName("GET /api/v1/geocode/reverse")
	class Reverse {

		@Test
		@DisplayName("좌표가 속한 지역의 이름을 준다")
		void areaName() {
			externalApi.photonReverseReturns("reverse-anbandegi-20261125.json");

			assertThat(reverse("37.623", "128.739")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": { "name": "강릉시 왕산면" }
				}
				""");
		}

		@Test
		@DisplayName("근처에 아무것도 없으면 name이 빠진다")
		void nothingNearby() {
			externalApi.photonReverseReturns("reverse-sea-20261125.json");

			assertThat(reverse("36.5", "130.5")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": {}
				}
				""");
		}

		@Test
		@DisplayName("100m 안의 좌표는 Photon을 다시 부르지 않는다")
		void cached() {
			externalApi.photonReverseReturns("reverse-anbandegi-20261125.json");
			reverse("37.623", "128.739");
			externalApi.reset();

			assertThat(reverse("37.6231", "128.7388")).hasStatusOk().bodyJson()
				.extractingPath("$.data.name").isEqualTo("강릉시 왕산면");
		}

		@Test
		@DisplayName("경도가 빠지면 400이다")
		void missingLongitude() {
			assertThat(mvc.get().uri("/api/v1/geocode/reverse").param("lat", "37.623").exchange())
				.hasStatus(400).bodyJson()
				.extractingPath("$.data.lon").isEqualTo("경도를 보내야 합니다.");
		}

		private MvcTestResult reverse(String lat, String lon) {
			return mvc.get().uri("/api/v1/geocode/reverse").param("lat", lat).param("lon", lon).exchange();
		}
	}
}
