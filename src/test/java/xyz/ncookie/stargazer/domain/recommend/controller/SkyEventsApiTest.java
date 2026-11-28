package xyz.ncookie.stargazer.domain.recommend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import xyz.ncookie.stargazer.support.IntegrationTestSupport;
import xyz.ncookie.stargazer.support.OpenMeteoNight;

/**
 * 은하수가 보이는 시간과 천문 현상. 목록은 실제 리소스(skyevents/sky_events.csv)를 쓴다.
 *
 * 쌍둥이자리 유성우의 밤(2026-12-14)이 예보 범위에 들어온 12월 8일에 목록 화면이 다가오는 현상을 묻고, 그 밤을 골라
 * 어디가 맑은지 보는 흐름을 재현한다. 시각은 지금 계산이 내는 값을 고정한 것이다.
 */
@SuppressWarnings("unchecked")
class SkyEventsApiTest extends IntegrationTestSupport {

	private static final LocalDate FIRST_NIGHT = LocalDate.of(2026, 12, 8);
	private static final LocalDate GEMINIDS_NIGHT = LocalDate.of(2026, 12, 14);

	// 강릉 안반데기 멍에전망대 주차장
	private static final double ANBANDEGI_LAT = 37.62633;
	private static final double ANBANDEGI_LON = 128.73942;

	@Test
	@DisplayName("쌍둥이자리 유성우의 밤이 예보 범위에 들면, 현상 목록에서 그 밤을 골라 어디가 맑은지 볼 수 있다")
	void geminidsNight() {
		clock.setTo(Instant.parse("2026-12-08T08:30:00Z")); // 한국 시간 오후 5시 30분
		// 안반데기만 유성우의 밤(일곱째 밤)에 맑다
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
		externalApi.openMeteoForecastReturnsBodyFor(
			ANBANDEGI_LAT, ANBANDEGI_LON, OpenMeteoNight.nights(FIRST_NIGHT, 95, 95, 95, 95, 95, 95, 0));
		externalApi.openMeteoForecastReturnsBodyForOthers(OpenMeteoNight.nights(FIRST_NIGHT, 95, 95, 95, 95, 95, 95, 95));

		// 목록 화면: 다가오는 현상
		String events = bodyOf(mvc.get().uri("/api/v1/recommendations/events?lat=37.5665&lon=126.978").exchange());
		Map<String, Object> first = JsonPath.read(events, "$.data.events[0]");
		assertThat(first)
			.containsEntry("night", "2026-12-14")
			.containsEntry("withinForecast", true);
		assertThat((Map<String, Object>) first.get("event"))
			.containsEntry("id", "2026-geminids")
			.containsEntry("type", "METEOR_SHOWER")
			.containsEntry("name", "쌍둥이자리 유성우")
			.containsEntry("peak", "2026-12-14T23:00:00+09:00")
			.containsEntry("zhr", 150);
		// 서울에서 복사점은 저녁에 10°를 넘어 새벽 박명까지 떠 있고, 2026년은 초승달이 일찍 진다
		assertThat((List<Map<String, String>>) JsonPath.read(events, "$.data.events[0].event.visible")).containsExactly(
			Map.of("start", "2026-12-14T19:32:00+09:00", "end", "2026-12-15T06:05:00+09:00"));
		assertThat((List<Map<String, String>>) JsonPath.read(events, "$.data.events[0].event.moonless")).containsExactly(
			Map.of("start", "2026-12-14T21:52:00+09:00", "end", "2026-12-15T06:05:00+09:00"));
		assertThat((Map<String, Object>) JsonPath.read(events, "$.data.events[0].moon")).isEqualTo(Map.of(
			"illuminatedFraction", 0.25, "waxing", true,
			"rise", "2026-12-15T11:36:00+09:00", "set", "2026-12-14T21:52:00+09:00"));

		// 밤 고르기: 일곱째 밤에만 현상이 붙는다
		String summary = bodyOf(mvc.get().uri("/api/v1/recommendations/nights").exchange());
		List<Map<String, Object>> nights = JsonPath.read(summary, "$.data.nights");
		assertThat(nights).hasSize(7);
		assertThat(nights.subList(0, 6)).allSatisfy(night -> assertThat(night).doesNotContainKey("events"));
		assertThat((List<String>) JsonPath.read(summary, "$.data.nights[6].events[*].id")).containsExactly("2026-geminids");
		// 자리를 정하지 않은 값이라 보이는 구간은 없다
		assertThat((Map<String, Object>) JsonPath.read(summary, "$.data.nights[6].events[0]"))
			.doesNotContainKeys("visible", "moonless");

		// 그 밤을 열면 맑은 안반데기가 맨 앞이고, 장소마다 복사점이 보이는 구간이 있다. 12월이라 은하수 중심은 뜨지 않는다
		String night = bodyOf(mvc.get().uri("/api/v1/recommendations/nights/" + GEMINIDS_NIGHT).exchange());
		Map<String, Object> best = JsonPath.read(night, "$.data.places[0]");
		assertThat(best).containsEntry("level", "STRONG").containsEntry("viewpoint", "멍에전망대 주차장");
		assertThat(best).doesNotContainKey("milkyWay");
		assertThat((List<String>) JsonPath.read(night, "$.data.places[0].events[*].id")).containsExactly("2026-geminids");
		assertThat((List<Object>) JsonPath.read(night, "$.data.places[0].events[0].visible")).hasSize(1);
		// 현상이 없는 밤의 장소에는 events가 빠진다
		String ordinary = bodyOf(mvc.get().uri("/api/v1/recommendations/nights/" + FIRST_NIGHT).exchange());
		assertThat((Map<String, Object>) JsonPath.read(ordinary, "$.data.places[0]")).doesNotContainKey("events");
	}

	@Test
	@DisplayName("예보가 닿지 않는 밤의 현상은 withinForecast가 false다")
	void beyondForecast() {
		clock.setTo(Instant.parse("2026-11-29T08:30:00Z"));

		String events = bodyOf(mvc.get().uri("/api/v1/recommendations/events?lat=37.5665&lon=126.978").exchange());

		assertThat((String) JsonPath.read(events, "$.data.startDate")).isEqualTo("2026-11-29");
		assertThat((String) JsonPath.read(events, "$.data.events[0].night")).isEqualTo("2026-12-14");
		assertThat((Boolean) JsonPath.read(events, "$.data.events[0].withinForecast")).isFalse();
	}

	@Test
	@DisplayName("위치를 보내지 않으면 보이는 구간과 달이 빠진다")
	void withoutLocation() {
		clock.setTo(Instant.parse("2026-12-08T08:30:00Z"));

		String events = bodyOf(mvc.get().uri("/api/v1/recommendations/events").exchange());

		Map<String, Object> first = JsonPath.read(events, "$.data.events[0]");
		assertThat(first).doesNotContainKey("moon");
		assertThat((Map<String, Object>) first.get("event")).doesNotContainKeys("visible", "moonless");
		assertThat((Map<String, Object>) JsonPath.read(events, "$.data")).doesNotContainKeys("latitude", "longitude");
	}

	@Test
	@DisplayName("지난 현상은 나오지 않는다. 목록이 끝나면 빈 배열이다")
	void pastEventsAreLeftOut() {
		clock.setTo(Instant.parse("2026-12-15T08:30:00Z"));

		String events = bodyOf(mvc.get().uri("/api/v1/recommendations/events").exchange());

		assertThat((List<Object>) JsonPath.read(events, "$.data.events")).isEmpty();
	}

	@Test
	@DisplayName("위도가 범위를 벗어나면 400이다")
	void invalidLatitude() {
		assertThat(mvc.get().uri("/api/v1/recommendations/events?lat=91&lon=127").exchange()).hasStatus(400);
	}

	@Test
	@DisplayName("여름 밤의 달력과 이번 주에는 은하수 중심이 보이는 시간이 있고, 다음으로 보이는 밤은 그 밤이다")
	void milkyWayInSummer() {
		clock.setTo(Instant.parse("2026-07-14T08:30:00Z")); // 7월 14일은 삭 무렵이다
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
		externalApi.openMeteoForecastReturnsBodyForOthers(
			OpenMeteoNight.nights(LocalDate.of(2026, 7, 14), 0, 0, 0, 0, 0, 0, 0));

		String moon = bodyOf(mvc.get().uri("/api/v1/recommendations/moon?lat=37.5665&lon=126.978").exchange());
		assertThat((String) JsonPath.read(moon, "$.data.milkyWayNextDate")).isEqualTo("2026-07-14");
		assertThat((List<Map<String, String>>) JsonPath.read(moon, "$.data.nights[0].milkyWay.visible")).containsExactly(
			Map.of("start", "2026-07-14T21:43:00+09:00", "end", "2026-07-15T01:57:00+09:00"));
		assertThat((List<Map<String, String>>) JsonPath.read(moon, "$.data.nights[0].milkyWay.moonless")).containsExactly(
			Map.of("start", "2026-07-14T21:43:00+09:00", "end", "2026-07-15T01:57:00+09:00"));

		String week = bodyOf(mvc.get().uri("/api/v1/recommendations/week?lat=37.5665&lon=126.978").exchange());
		assertThat((Object) JsonPath.read(week, "$.data.places[0].nights[0].milkyWay"))
			.isEqualTo(JsonPath.read(moon, "$.data.nights[0].milkyWay"));
	}

	@Test
	@DisplayName("겨울의 달력에는 은하수 중심이 다시 보이기 시작하는 밤이 온다")
	void milkyWayReturnsAfterWinter() {
		clock.setTo(Instant.parse("2026-12-08T08:30:00Z"));

		String moon = bodyOf(mvc.get().uri("/api/v1/recommendations/moon?lat=37.5665&lon=126.978").exchange());

		assertThat((Map<String, Object>) JsonPath.read(moon, "$.data.nights[0]")).doesNotContainKey("milkyWay");
		assertThat(LocalDate.parse(JsonPath.read(moon, "$.data.milkyWayNextDate")))
			.isBetween(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 15));
		// 쌍둥이자리 유성우의 밤은 달력에도 붙는다
		assertThat((List<String>) JsonPath.read(moon, "$.data.nights[6].events[*].id")).containsExactly("2026-geminids");
	}

	private static String bodyOf(MvcTestResult result) {
		assertThat(result).hasStatusOk();
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
