package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 달의 모양과 뜨고 지는 시각을 실제 날짜로 확인한다.
 *
 * 기준은 2026-11-09의 삭과 2026-11-24의 망이다(USNO, https://aa.usno.navy.mil/data/MoonPhases).
 * 뜨고 지는 시각은 분 단위 값을 고정하지 않고, 위상에서 따라 나오는 범위로 본다.
 * 보름달은 해가 질 무렵 떠서 해가 뜰 무렵 지고, 삭의 달은 해와 함께 뜨고 진다.
 */
class SkyEphemerisMoonTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final double LAT = 37.5665;
	private static final double LON = 126.9780;

	private final SkyEphemeris sky = new SkyEphemeris();

	@Test
	@DisplayName("망의 밤에는 달이 거의 다 차 있고, 삭의 밤에는 거의 보이지 않는다")
	void fullAndNew() {
		assertThat(sky.moonIlluminatedFraction(seoul(2026, 11, 25, 0))).isGreaterThan(0.99);
		assertThat(sky.moonIlluminatedFraction(seoul(2026, 11, 10, 0))).isLessThan(0.02);
	}

	@Test
	@DisplayName("삭과 망의 가운데에서는 반쯤 찬 달이 차오르는 중이다")
	void waxingHalf() {
		Instant betweenNewAndFull = seoul(2026, 11, 17, 0);

		assertThat(sky.isMoonWaxing(betweenNewAndFull)).isTrue();
		assertThat(sky.moonIlluminatedFraction(betweenNewAndFull)).isBetween(0.3, 0.7);
	}

	@Test
	@DisplayName("망에서 한 주가 지나면 반쯤 남은 달이 기우는 중이다")
	void waningHalf() {
		Instant weekAfterFull = seoul(2026, 12, 2, 0);

		assertThat(sky.isMoonWaxing(weekAfterFull)).isFalse();
		assertThat(sky.moonIlluminatedFraction(weekAfterFull)).isBetween(0.3, 0.7);
	}

	@Test
	@DisplayName("보름달은 해 질 무렵 떠서 다음 날 해 뜰 무렵 진다")
	void fullMoonRisesAtDusk() {
		Instant noon = seoul(2026, 11, 24, 12);

		assertThat(sky.moonrise(LAT, LON, noon)).get()
			.satisfies(rise -> assertThat(rise).isBetween(seoul(2026, 11, 24, 16), seoul(2026, 11, 24, 19)));
		assertThat(sky.moonset(LAT, LON, noon)).get()
			.satisfies(set -> assertThat(set).isBetween(seoul(2026, 11, 25, 6), seoul(2026, 11, 25, 9)));
	}

	@Test
	@DisplayName("삭의 달은 해와 함께 저녁에 지고 다음 날 아침에 뜬다")
	void newMoonSetsAtDusk() {
		Instant noon = seoul(2026, 11, 9, 12);

		assertThat(sky.moonset(LAT, LON, noon)).get()
			.satisfies(set -> assertThat(set).isBetween(seoul(2026, 11, 9, 16), seoul(2026, 11, 9, 19)));
		assertThat(sky.moonrise(LAT, LON, noon)).get()
			.satisfies(rise -> assertThat(rise).isBetween(seoul(2026, 11, 10, 6), seoul(2026, 11, 10, 10)));
	}

	private static Instant seoul(int year, int month, int day, int hour) {
		return LocalDateTime.of(year, month, day, hour, 0).atZone(SEOUL).toInstant();
	}
}
