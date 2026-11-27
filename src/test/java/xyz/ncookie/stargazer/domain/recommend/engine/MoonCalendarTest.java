package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.engine.SkyEphemeris.Crossings;
import xyz.ncookie.stargazer.domain.recommend.model.MoonNight;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;

/**
 * 달력의 달 없는 시간을 실제 날짜로 확인한다.
 *
 * 기준은 2026-11-09의 삭과 2026-11-24의 망이다(USNO, https://aa.usno.navy.mil/data/MoonPhases).
 * 상현과 하현의 밤은 그 가운데쯤인 11-17과 12-01로 잡았다. 시각은 분 단위로 고정하지 않고 위상에서 따라 나오는 범위로 본다.
 * 상현달은 자정 무렵 지고, 하현달은 자정 무렵 뜬다.
 */
class MoonCalendarTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final double LAT = 37.5665;
	private static final double LON = 126.9780;

	private final MoonCalendar calendar = new MoonCalendar(new SkyEphemeris());

	@Test
	@DisplayName("삭의 밤에는 어두운 시간 내내 달이 없다")
	void newMoon() {
		MoonNight night = night(2026, 11, 9);

		assertThat(night.moonless()).containsExactly(night.dark());
	}

	@Test
	@DisplayName("보름의 밤에는 달 없이 어두운 시간이 없다")
	void fullMoon() {
		MoonNight night = night(2026, 11, 24);

		assertThat(night.dark()).isNotNull();
		assertThat(night.moonless()).isEmpty();
	}

	@Test
	@DisplayName("상현의 밤에는 달이 자정 무렵 진 뒤부터 새벽까지가 어둡고, 시작은 월몰 시각과 같다")
	void firstQuarter() {
		MoonNight night = night(2026, 11, 17);

		assertThat(night.moonless()).hasSize(1);
		TimeSpan span = night.moonless().get(0);
		assertThat(span.start()).isEqualTo(night.moon().set());
		assertThat(span.start()).isBetween(seoul(2026, 11, 17, 23), seoul(2026, 11, 18, 1));
		assertThat(span.end()).isEqualTo(night.dark().end());
	}

	@Test
	@DisplayName("하현의 밤에는 초저녁부터 달이 자정 무렵 뜰 때까지가 어둡고, 끝은 월출 시각과 같다")
	void lastQuarter() {
		MoonNight night = night(2026, 12, 1);

		assertThat(night.moonless()).hasSize(1);
		TimeSpan span = night.moonless().get(0);
		assertThat(span.start()).isEqualTo(night.dark().start());
		assertThat(span.end()).isEqualTo(night.moon().rise());
		assertThat(span.end()).isBetween(seoul(2026, 12, 1, 23), seoul(2026, 12, 2, 2));
	}

	@Test
	@DisplayName("어두운 구간은 저녁에 시작해 다음 날 새벽에 끝난다")
	void darkSpansTheNight() {
		MoonNight night = night(2026, 11, 9);

		assertThat(night.dark().start()).isBetween(seoul(2026, 11, 9, 18), seoul(2026, 11, 9, 19));
		assertThat(night.dark().end()).isBetween(seoul(2026, 11, 10, 5), seoul(2026, 11, 10, 7));
	}

	@Test
	@DisplayName("첫 날부터 이어지는 날짜로 밤의 수만큼 돌려준다")
	void consecutiveNights() {
		List<MoonNight> nights = calendar.nights(LAT, LON, LocalDate.of(2026, 11, 9), 42, SEOUL);

		assertThat(nights).hasSize(42);
		assertThat(nights.get(0).date()).isEqualTo(LocalDate.of(2026, 11, 9));
		assertThat(nights.get(41).date()).isEqualTo(LocalDate.of(2026, 12, 20));
	}

	@Nested
	@DisplayName("기준선 아래 구간 나누기")
	class SpansBelow {

		private final Instant from = Instant.parse("2026-11-09T09:00:00Z");
		private final Instant to = Instant.parse("2026-11-09T21:00:00Z");

		@Test
		@DisplayName("아래에서 시작해 떠올랐다가 지면 앞뒤 두 구간이다")
		void downRiseSet() {
			Instant rise = at(2);
			Instant set = at(8);

			assertThat(MoonCalendar.spansBelow(from, to, new Crossings(rise, set, false, false)))
				.containsExactly(new TimeSpan(from, rise), new TimeSpan(set, to));
		}

		@Test
		@DisplayName("위에서 시작해 졌다가 다시 뜨면 가운데 한 구간이다")
		void upSetRise() {
			Instant set = at(3);
			Instant rise = at(9);

			assertThat(MoonCalendar.spansBelow(from, to, new Crossings(rise, set, false, false)))
				.containsExactly(new TimeSpan(set, rise));
		}

		@Test
		@DisplayName("뜨기만 하면 처음부터 뜰 때까지, 지기만 하면 질 때부터 끝까지다")
		void singleCrossing() {
			assertThat(MoonCalendar.spansBelow(from, to, new Crossings(at(4), null, false, false)))
				.containsExactly(new TimeSpan(from, at(4)));
			assertThat(MoonCalendar.spansBelow(from, to, new Crossings(null, at(4), false, false)))
				.containsExactly(new TimeSpan(at(4), to));
		}

		@Test
		@DisplayName("내내 위에 있으면 구간이 없고, 내내 아래에 있으면 전체가 한 구간이다")
		void noCrossing() {
			assertThat(MoonCalendar.spansBelow(from, to, new Crossings(null, null, true, false))).isEmpty();
			assertThat(MoonCalendar.spansBelow(from, to, new Crossings(null, null, false, true)))
				.containsExactly(new TimeSpan(from, to));
		}

		private Instant at(int hours) {
			return from.plusSeconds(hours * 3600L);
		}
	}

	private MoonNight night(int year, int month, int day) {
		return calendar.night(LAT, LON, LocalDate.of(year, month, day), SEOUL);
	}

	private static Instant seoul(int year, int month, int day, int hour) {
		return LocalDateTime.of(year, month, day, hour, 0).atZone(SEOUL).toInstant();
	}
}
