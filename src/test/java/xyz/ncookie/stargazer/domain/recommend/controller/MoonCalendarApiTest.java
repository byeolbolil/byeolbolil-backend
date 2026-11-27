package xyz.ncookie.stargazer.domain.recommend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 한 좌표의 달력. 해와 달의 위치만 계산하므로 외부 API 대체 응답을 등록하지 않는다. 부르면 테스트가 실패한다.
 * 2026-11-09는 삭이다(USNO, https://aa.usno.navy.mil/data/MoonPhases). 시각은 지금 계산이 내는 값을 고정한 것이다.
 */
class MoonCalendarApiTest extends IntegrationTestSupport {

	@Test
	@DisplayName("오늘 밤부터 6주의 밤을 주고, 삭의 밤은 어두운 시간 내내 달이 없다")
	void newMoonNight() {
		clock.setTo(Instant.parse("2026-11-09T08:30:00Z")); // 한국 시간 오후 5시 30분

		MvcTestResult result = moon("37.5665", "126.978");

		assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
			{
			  "success": true,
			  "status": 200,
			  "data": { "latitude": 37.5665, "longitude": 126.978, "startDate": "2026-11-09" }
			}
			""");
		assertThat(result).bodyJson().extractingPath("$.data.nights[0]").isEqualTo(Map.of(
			"date", "2026-11-09",
			"moon", Map.of(
				"illuminatedFraction", 0.0,
				"waxing", true,
				"rise", "2026-11-10T08:01:00+09:00",
				"set", "2026-11-09T16:59:00+09:00"),
			"dark", Map.of("start", "2026-11-09T18:25:00+09:00", "end", "2026-11-10T06:07:00+09:00"),
			"moonless", List.of(Map.of("start", "2026-11-09T18:25:00+09:00", "end", "2026-11-10T06:07:00+09:00")),
			"moonlessMinutes", 702));
		assertThat(result).bodyJson().extractingPath("$.data.nights.length()").isEqualTo(42);
		assertThat(result).bodyJson().extractingPath("$.data.nights[41].date").isEqualTo("2026-12-20");
	}

	@Test
	@DisplayName("보름의 밤은 달 없는 구간이 빈 배열이고 0분이다")
	void fullMoonNight() {
		clock.setTo(Instant.parse("2026-11-24T08:30:00Z"));

		MvcTestResult result = moon("37.5665", "126.978");

		assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data.startDate").isEqualTo("2026-11-24");
		assertThat(result).bodyJson().extractingPath("$.data.nights[0].moonless").asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.data.nights[0].moonlessMinutes").isEqualTo(0);
	}

	@Test
	@DisplayName("새벽 4시 전에는 지난 저녁에 시작한 밤부터 준다")
	void beforeRollover() {
		clock.setTo(Instant.parse("2026-11-09T18:30:00Z")); // 한국 시간 10일 새벽 3시 30분

		MvcTestResult result = moon("37.5665", "126.978");

		assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data.startDate").isEqualTo("2026-11-09");
	}

	@Test
	@DisplayName("경도가 빠지면 400이다")
	void missingLongitude() {
		MvcTestResult result = mvc.get().uri("/api/v1/recommendations/moon?lat=37.6").exchange();

		assertThat(result).hasStatus(400);
	}

	private MvcTestResult moon(String lat, String lon) {
		return mvc.get().uri("/api/v1/recommendations/moon?lat={lat}&lon={lon}", lat, lon).exchange();
	}
}
