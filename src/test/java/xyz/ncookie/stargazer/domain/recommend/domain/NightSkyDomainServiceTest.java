package xyz.ncookie.stargazer.domain.recommend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.engine.SkyEphemeris;
import xyz.ncookie.stargazer.domain.recommend.engine.TargetVisibility;
import xyz.ncookie.stargazer.domain.recommend.model.NightEvent;
import xyz.ncookie.stargazer.domain.recommend.model.NightSky;
import xyz.ncookie.stargazer.domain.skyevent.model.EclipseKind;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEventType;

class NightSkyDomainServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final double LAT = 37.5;
	private static final double LON = 127.0;

	private static final SkyEvent GEMINIDS = new SkyEvent(
		"2026-geminids", SkyEventType.METEOR_SHOWER, "쌍둥이자리 유성우", LocalDate.of(2026, 12, 14),
		Instant.parse("2026-12-14T14:00:00Z"), 112.0, 33.0, 150, null, null, null, "test");

	private static final SkyEvent ECLIPSE = new SkyEvent(
		"2026-lunar-eclipse-0303", SkyEventType.LUNAR_ECLIPSE, "개기월식", LocalDate.of(2026, 3, 3),
		Instant.parse("2026-03-03T11:33:42Z"), null, null, null, EclipseKind.TOTAL,
		Instant.parse("2026-03-03T09:49:48Z"), Instant.parse("2026-03-03T13:17:36Z"), "test");

	private final NightSkyDomainService service = new NightSkyDomainService(new TargetVisibility(new SkyEphemeris()), new SkyEphemeris());

	@Test
	@DisplayName("겨울에 다음으로 은하수 중심이 보이는 밤은 새벽에 다시 뜨기 시작하는 늦겨울이다")
	void nextMilkyWayAfterWinter() {
		LocalDate next = service.nextMilkyWayNight(LAT, LON, LocalDate.of(2026, 12, 1), SEOUL).orElseThrow();

		assertThat(next).isBetween(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 15));
		assertThat(service.milkyWay(LAT, LON, next.minusDays(1), SEOUL)).isEmpty();
	}

	@Test
	@DisplayName("은하수가 보이는 철에는 다음으로 보이는 밤이 그 밤이다")
	void nextMilkyWayInSummer() {
		assertThat(service.nextMilkyWayNight(LAT, LON, LocalDate.of(2026, 7, 1), SEOUL))
			.contains(LocalDate.of(2026, 7, 1));
	}

	@Test
	@DisplayName("쌍둥이자리 유성우의 밤에는 복사점이 저녁에 떠서 새벽까지 보이고, 달이 없다")
	void geminidsRadiant() {
		NightEvent event = service.nightEvent(GEMINIDS, LAT, LON, SEOUL);

		assertThat(event.radiant()).isNotNull();
		assertThat(event.radiant().spans()).hasSize(1);
		// 복사점은 적위 +33°라 저녁 8시 무렵에 10°를 넘고 새벽 박명까지 지지 않는다
		assertThat(event.radiant().spans().get(0).duration().toHours()).isGreaterThanOrEqualTo(9);
		// 2026년은 극대 무렵 초승달이라 일찍 진다(IMO 2026 달력의 "moon-free maximum")
		assertThat(event.radiant().moonless()).isNotEmpty();
		assertThat(event.eclipse()).isEmpty();
	}

	@Test
	@DisplayName("월식은 복사점이 없고, 달이 떠 있는 동안의 월식 구간이 나온다")
	void eclipseSpan() {
		NightEvent event = service.nightEvent(ECLIPSE, LAT, LON, SEOUL);

		assertThat(event.radiant()).isNull();
		assertThat(event.eclipse()).hasSize(1);
	}

	@Test
	@DisplayName("밤하늘에는 그 밤의 은하수와 넘겨받은 현상이 담긴다")
	void nightSky() {
		NightSky night = service.nightSky(LAT, LON, LocalDate.of(2026, 12, 14), SEOUL, List.of(GEMINIDS));

		assertThat(night.milkyWay()).isNull();
		assertThat(night.events()).extracting(e -> e.event().id()).containsExactly("2026-geminids");
	}
}
