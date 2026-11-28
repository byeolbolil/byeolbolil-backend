package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;
import xyz.ncookie.stargazer.domain.recommend.model.VisibleSpans;

class TargetVisibilityTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final double LAT = 37.5;
	private static final double LON = 127.0;

	private final SkyEphemeris sky = new SkyEphemeris();
	private final TargetVisibility visibility = new TargetVisibility(sky);

	@Test
	@DisplayName("하지의 밤에는 은하수 중심이 보이고, 구간 안에서는 하늘이 완전히 어둡고 중심이 10° 위에 있다")
	void summerSolstice() {
		VisibleSpans night = milkyWay(LocalDate.of(2026, 6, 21));

		assertThat(night.spans()).hasSize(1);
		TimeSpan span = night.spans().get(0);
		assertThat(span.duration()).isGreaterThan(Duration.ofHours(4));

		Instant middle = span.start().plus(span.duration().dividedBy(2));
		assertThat(altitude(middle)).isGreaterThan(TargetVisibility.MIN_ALTITUDE);
		assertThat(sky.sunAltitude(LAT, LON, middle)).isLessThan(NightAssessmentEngine.FULLY_DARK_SUN_ALTITUDE);

		// 끝은 하늘이 밝아지기 시작하는 때이거나 중심이 10° 아래로 내려가는 때다
		assertThat(isEdge(span.start())).isTrue();
		assertThat(isEdge(span.end())).isTrue();
	}

	@Test
	@DisplayName("동지 무렵에는 은하수 중심이 해와 같은 쪽에 있어 밤에 뜨지 않는다")
	void winterSolstice() {
		assertThat(visibility.night(MilkyWay.CENTER_RA, MilkyWay.CENTER_DEC, LAT, LON, LocalDate.of(2026, 12, 21), SEOUL))
			.isEmpty();
	}

	@Test
	@DisplayName("한 번도 기준 고도에 오르지 않는 점은 어느 밤에도 보이지 않는다")
	void neverRises() {
		assertThat(visibility.night(0.0, -60.0, LAT, LON, LocalDate.of(2026, 6, 21), SEOUL)).isEmpty();
	}

	@Test
	@DisplayName("보름달이 밤새 떠 있는 밤에는 은하수 중심이 보여도 달 없는 구간이 없다")
	void fullMoonAllNight() {
		// 2026년 7월 29일은 보름이다. 달이 해 질 녘에 떠서 해 뜰 녘에 진다
		VisibleSpans night = milkyWay(LocalDate.of(2026, 7, 29));

		assertThat(night.spans()).isNotEmpty();
		assertThat(night.moonless()).isEmpty();
	}

	@Test
	@DisplayName("달 없는 구간은 보이는 구간 안에 있고, 그 안에서 달은 지평선 아래에 있다")
	void moonlessInsideVisible() {
		// 2026년 6월 15일이 삭이라 6월 21일의 상현 전 달은 밤중에 진다
		VisibleSpans night = milkyWay(LocalDate.of(2026, 6, 21));

		assertThat(night.moonless()).isNotEmpty();
		for (TimeSpan moonless : night.moonless()) {
			assertThat(night.spans()).anySatisfy(span -> {
				assertThat(moonless.start()).isAfterOrEqualTo(span.start());
				assertThat(moonless.end()).isBeforeOrEqualTo(span.end());
			});
			Instant middle = moonless.start().plus(moonless.duration().dividedBy(2));
			assertThat(sky.moonAltitude(LAT, LON, middle)).isLessThan(0.0);
		}
	}

	@Test
	@DisplayName("시간각으로 셈한 구간은 10분마다 고도를 계산한 결과와 맞다")
	void spansAboveMatchSampling() {
		Instant from = Instant.parse("2026-03-01T03:00:00Z");
		Instant to = from.plus(Duration.ofDays(2));
		List<TimeSpan> spans = TargetVisibility.spansAbove(
			MilkyWay.CENTER_RA, MilkyWay.CENTER_DEC, LAT, LON, from, to, TargetVisibility.MIN_ALTITUDE);

		// 이틀이면 두 번 뜬다
		assertThat(spans).hasSize(2);
		for (Instant t = from; t.isBefore(to); t = t.plus(Duration.ofMinutes(10))) {
			Instant time = t;
			boolean inside = spans.stream().anyMatch(s -> !time.isBefore(s.start()) && time.isBefore(s.end()));
			assertThat(inside).as("%s 고도 %.3f°", time, altitude(time))
				.isEqualTo(altitude(time) > TargetVisibility.MIN_ALTITUDE);
		}
		for (TimeSpan span : spans) {
			assertThat(altitude(span.start())).isCloseTo(TargetVisibility.MIN_ALTITUDE, within(0.01));
			assertThat(altitude(span.end())).isCloseTo(TargetVisibility.MIN_ALTITUDE, within(0.01));
		}
	}

	@Test
	@DisplayName("월식 동안 달이 지평선 위에 있는 부분만 남긴다")
	void moonUpDuringEclipse() {
		// 2026년 3월 3일 개기월식. 한국에서는 달이 뜬 뒤로 끝까지 보인다(한국천문연구원). 부분식 18:49:48~22:17:36(KST)
		TimeSpan eclipse = new TimeSpan(Instant.parse("2026-03-03T09:49:48Z"), Instant.parse("2026-03-03T13:17:36Z"));

		List<TimeSpan> up = visibility.moonUp(LAT, LON, eclipse);

		assertThat(up).hasSize(1);
		assertThat(up.get(0).end()).isEqualTo(eclipse.end());
		// 서울의 그날 월출은 18시 안팎이라 부분식이 시작할 때 달은 이미 떠 있다
		assertThat(up.get(0).start()).isEqualTo(eclipse.start());
	}

	@Test
	@DisplayName("월식이 끝나기 전에 달이 지면 진 뒤의 부분은 빠진다")
	void moonSetsDuringEclipse() {
		// 보름 다음 날 아침의 구간을 월식처럼 넣어 본다. 달은 해가 뜰 무렵 진다
		TimeSpan morning = new TimeSpan(Instant.parse("2026-03-03T22:00:00Z"), Instant.parse("2026-03-04T03:00:00Z"));
		Instant moonset = sky.moonset(LAT, LON, morning.start()).orElseThrow();

		List<TimeSpan> up = visibility.moonUp(LAT, LON, morning);

		assertThat(moonset).isBetween(morning.start(), morning.end());
		assertThat(up).containsExactly(new TimeSpan(morning.start(), moonset));
	}

	private VisibleSpans milkyWay(LocalDate date) {
		return visibility.night(MilkyWay.CENTER_RA, MilkyWay.CENTER_DEC, LAT, LON, date, SEOUL).orElseThrow();
	}

	private double altitude(Instant time) {
		return HorizontalCoordinates.altitude(MilkyWay.CENTER_RA, MilkyWay.CENTER_DEC, LAT, LON, time);
	}

	private boolean isEdge(Instant time) {
		return Math.abs(altitude(time) - TargetVisibility.MIN_ALTITUDE) < 0.05
			|| Math.abs(sky.sunAltitude(LAT, LON, time) - NightAssessmentEngine.FULLY_DARK_SUN_ALTITUDE) < 0.1;
	}
}
