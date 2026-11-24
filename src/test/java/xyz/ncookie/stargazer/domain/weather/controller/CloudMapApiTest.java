package xyz.ncookie.stargazer.domain.weather.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import xyz.ncookie.stargazer.domain.weather.model.CloudGridSpec;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 구름 격자 API의 응답을 고정한다.
 *
 * Open-Meteo의 격자 응답은 306점이라 파일로 두지 않고 테스트가 만든다. 형식은 실제 응답
 * (fixtures/openmeteo/cloud-grid-2x3-ecmwf-ifs-20261124.json)과 같다. 점마다 값이 달라서 순서가 바뀌면 드러난다.
 */
class CloudMapApiTest extends IntegrationTestSupport {

	// 모델 메타 fixture의 실행 시각이 2026-11-23T06:00Z라서 그 뒤의 시각으로 맞춘다
	private static final Instant NOW = Instant.parse("2026-11-23T18:50:00Z");
	private static final Instant DATA_START = Instant.parse("2026-11-23T00:00:00Z");
	private static final int DATA_HOURS = 72;
	private static final CloudGridSpec GRID = CloudGridSpec.KOREA;

	@BeforeEach
	void setUp() {
		clock.setTo(NOW);
		externalApi.openMeteoModelMetaReturns("meta-ecmwf-ifs-20261123.json");
	}

	@Test
	@DisplayName("로그인 없이 격자와, 저녁 5시부터 다음 날 아침 8시 전까지의 값을 준다")
	void night() {
		externalApi.openMeteoCloudGridReturnsBody(gridBody());

		MvcTestResult result = get("2026-11-24");

		assertThat(result).hasStatusOk();
		String body = body(result);
		assertThat(JsonPath.<String>read(body, "$.data.date")).isEqualTo("2026-11-24");
		assertThat(JsonPath.<String>read(body, "$.data.source")).isEqualTo("OPEN_METEO");
		assertThat(JsonPath.<String>read(body, "$.data.issuedAt")).isEqualTo("2026-11-23T06:00:00Z");
		assertThat(result).bodyJson().extractingPath("$.data.grid").isEqualTo(
			java.util.Map.of("south", 32.6, "west", 124.2, "step", 0.4, "rows", 17, "columns", 18));

		List<String> times = JsonPath.read(body, "$.data.hours[*].time");
		assertThat(times).hasSize(15);
		assertThat(times.get(0)).isEqualTo("2026-11-24T17:00:00+09:00");
		assertThat(times.get(14)).isEqualTo("2026-11-25T07:00:00+09:00");
	}

	@Test
	@DisplayName("값은 남쪽 줄부터, 줄 안에서는 서쪽부터 306개다")
	void order() {
		externalApi.openMeteoCloudGridReturnsBody(gridBody());

		String body = body(get("2026-11-24"));

		// 2026-11-24 17:00 KST는 자료의 32번째 시각이다
		List<Integer> first = JsonPath.read(body, "$.data.hours[0].cloudCover");
		assertThat(first).hasSize(306);
		assertThat(first.get(0)).isEqualTo(cover(0, 32));
		assertThat(first.get(1)).isEqualTo(cover(1, 32));
		assertThat(first.get(18)).isEqualTo(cover(18, 32));
		assertThat(first.get(305)).isEqualTo(cover(305, 32));
	}

	@Test
	@DisplayName("같은 모델 실행이면 다른 날짜를 물어도 Open-Meteo를 다시 부르지 않는다")
	void cachedPerRun() {
		externalApi.openMeteoCloudGridReturnsBody(gridBody());
		get("2026-11-24");
		// 이 뒤로는 어떤 외부 호출도 테스트를 실패시킨다
		externalApi.reset();

		assertThat(get("2026-11-23")).hasStatusOk().bodyJson()
			.extractingPath("$.data.hours[0].time").isEqualTo("2026-11-23T17:00:00+09:00");
	}

	@Test
	@DisplayName("예보가 닿지 않는 날짜면 hours가 빈 배열이다")
	void outOfRange() {
		externalApi.openMeteoCloudGridReturnsBody(gridBody());

		assertThat(get("2026-12-24")).hasStatusOk().bodyJson()
			.extractingPath("$.data.hours").asArray().isEmpty();
	}

	@Test
	@DisplayName("격자 예보를 받지 못하면 503이다")
	void providerDown() {
		externalApi.openMeteoForecastFails();

		assertThat(get("2026-11-24")).hasStatus(503).bodyJson().isStrictlyEqualTo("""
			{
			  "success": false,
			  "status": 503,
			  "message": "날씨 예보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."
			}
			""");
	}

	@Test
	@DisplayName("날짜 형식이 틀리면 400이다")
	void badDate() {
		assertThat(get("내일")).hasStatus(400);
	}

	private MvcTestResult get(String date) {
		return mvc.get().uri("/api/v1/clouds/nights/{date}", date).exchange();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	/** 점과 시각마다 다른 값. 0~100 안에 든다 */
	private static int cover(int point, int hour) {
		return (point * 7 + hour * 3) % 101;
	}

	private static String gridBody() {
		String times = IntStream.range(0, DATA_HOURS)
			.mapToObj(hour -> String.valueOf(DATA_START.getEpochSecond() + 3600L * hour))
			.collect(Collectors.joining(","));
		return IntStream.range(0, GRID.size())
			.mapToObj(point -> {
				String covers = IntStream.range(0, DATA_HOURS)
					.mapToObj(hour -> String.valueOf(cover(point, hour)))
					.collect(Collectors.joining(","));
				return "{\"latitude\": %s, \"longitude\": %s, \"hourly\": {\"time\": [%s], \"cloud_cover\": [%s]}}".formatted(
					GRID.latitudeAt(point / GRID.columns()), GRID.longitudeAt(point % GRID.columns()), times, covers);
			})
			.collect(Collectors.joining(",", "[", "]"));
	}
}
