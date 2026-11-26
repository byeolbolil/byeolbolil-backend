package xyz.ncookie.stargazer.domain.recommend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 한 좌표의 하늘 밝기. 클래스패스의 실제 하늘 밝기 격자를 읽으므로 값은 격자를 다시 만들면 바뀐다.
 * 예보를 부르지 않으므로 외부 API 대체 응답을 등록하지 않는다. 부르면 테스트가 실패한다.
 */
class SkyApiTest extends IntegrationTestSupport {

	@Test
	@DisplayName("어두운 산 위는 시골 하늘이고 둘레가 밝지 않다")
	void darkMountain() {
		// 강릉 안반데기 멍에전망대 주차장
		MvcTestResult result = sky("37.62633", "128.73942");

		assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
			{
			  "success": true,
			  "status": 200,
			  "data": {
			    "latitude": 37.62633,
			    "longitude": 128.73942,
			    "skyBrightness": 3.11,
			    "skyClass": "RURAL",
			    "skyRank": 22,
			    "litSurroundings": false
			  }
			}
			""");
	}

	@Test
	@DisplayName("도시 한가운데는 도시 하늘이고 둘레가 밝다")
	void cityCenter() {
		// 서울시청
		MvcTestResult result = sky("37.5665", "126.978");

		assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
			{ "data": { "skyBrightness": 66.94, "skyClass": "URBAN", "skyRank": 100, "litSurroundings": true } }
			""");
	}

	@Test
	@DisplayName("자료가 있는 범위 밖이면 값을 빼고 200으로 답한다")
	void outsideGrid() {
		MvcTestResult result = sky("35.0", "140.0");

		assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
			{ "data": { "latitude": 35.0, "longitude": 140.0 } }
			""");
		assertThat(result).bodyJson().doesNotHavePath("$.data.skyBrightness");
		assertThat(result).bodyJson().doesNotHavePath("$.data.skyClass");
		assertThat(result).bodyJson().doesNotHavePath("$.data.skyRank");
		assertThat(result).bodyJson().doesNotHavePath("$.data.litSurroundings");
	}

	@Test
	@DisplayName("경도가 빠지면 400이다")
	void missingLongitude() {
		MvcTestResult result = mvc.get().uri("/api/v1/recommendations/sky?lat=37.6").exchange();

		assertThat(result).hasStatus(400);
	}

	private MvcTestResult sky(String lat, String lon) {
		return mvc.get().uri("/api/v1/recommendations/sky?lat={lat}&lon={lon}", lat, lon).exchange();
	}
}
