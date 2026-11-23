package xyz.ncookie.stargazer.domain.stargazing.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.stargazing.enums.MoonPhase;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;

/**
 * commons-suncalc로 구한 천문 값이 실제 날짜와 맞는지, 그리고 지금 어떤 값을 내보내는지 고정한다.
 *
 * 달 위상 시각은 U.S. Naval Observatory의 2026년 자료를 썼다(UT).
 * https://aa.usno.navy.mil/api/moon/phases/year?year=2026
 * 망 1월 3일 10:03, 하현 1월 10일 15:48, 삭 1월 18일 19:52, 상현 1월 26일 04:47.
 */
class AstronomyCalculatorTest {

	// 강릉 안반데기
	private static final double LAT = 37.6254;
	private static final double LON = 128.9392;
	private static final ZoneId UTC = ZoneId.of("UTC");
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final AstronomyCalculator calculator = new AstronomyCalculator();

	@Nested
	@DisplayName("달 위상")
	class MoonPhases {

		@Test
		@DisplayName("망 시각에는 빛나는 면적이 거의 1이고 위상 이름이 보름달이다")
		void fullMoon() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 3, 10, 3, 0, 0, UTC));

			assertThat(astro.moonFraction()).isGreaterThan(0.99);
			assertThat(MoonPhase.calculate(astro.moonPhaseDegree())).isEqualTo(MoonPhase.FULL_MOON);
		}

		@Test
		@DisplayName("삭 시각에는 빛나는 면적이 거의 0이고 위상 이름이 삭이다")
		void newMoon() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 18, 19, 52, 0, 0, UTC));

			assertThat(astro.moonFraction()).isLessThan(0.01);
			assertThat(MoonPhase.calculate(astro.moonPhaseDegree())).isEqualTo(MoonPhase.NEW_MOON);
		}

		@Test
		@DisplayName("상현 시각에는 위상각이 -90°이고 위상 이름이 상현달이다")
		void firstQuarter() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 26, 4, 47, 0, 0, UTC));

			assertThat(astro.moonFraction()).isCloseTo(0.5, within(0.01));
			assertThat(astro.moonPhaseDegree()).isCloseTo(-90.0, within(0.5));
			assertThat(MoonPhase.calculate(astro.moonPhaseDegree())).isEqualTo(MoonPhase.FIRST_QUARTER);
		}

		@Test
		@DisplayName("하현 시각에는 위상각이 +90°이고 위상 이름이 하현달이다")
		void lastQuarter() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 10, 15, 48, 0, 0, UTC));

			assertThat(astro.moonFraction()).isCloseTo(0.5, within(0.01));
			assertThat(astro.moonPhaseDegree()).isCloseTo(90.0, within(0.5));
			assertThat(MoonPhase.calculate(astro.moonPhaseDegree())).isEqualTo(MoonPhase.LAST_QUARTER);
		}
	}

	@Nested
	@DisplayName("달 고도")
	class MoonAltitude {

		@Test
		@DisplayName("보름달은 자정 무렵 높이 떠 있다")
		void fullMoonIsHighAtMidnight() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 3, 23, 0, 0, 0, SEOUL));

			assertThat(astro.moonAltitude()).isGreaterThan(60.0);
		}

		@Test
		@DisplayName("그믐 무렵의 달은 자정에 지평선 아래에 있다")
		void oldMoonIsDownAtMidnight() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 15, 23, 0, 0, 0, SEOUL));

			assertThat(astro.moonAltitude()).isNegative();
		}
	}

	@Nested
	@DisplayName("뜨고 지는 시각")
	class RiseAndSet {

		@Test
		@DisplayName("요청한 시각대의 HH:mm으로 내보낸다")
		void formattedInRequestZone() {
			RawAstronomyData astro = at(ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, SEOUL));

			assertThat(astro.sunset()).isEqualTo("17:29");
			assertThat(astro.sunrise()).isEqualTo("07:37");
		}

		@Test
		@DisplayName("그날의 일출과 일몰이 아니라 요청 시각 뒤에 처음 오는 일출과 일몰이다")
		void nextEventAfterRequestedTime() {
			// 1월 15일 일몰은 17:29, 16일 일몰은 17:30이다. 15일 밤에 물으면 이미 지난 15일 일몰이 아니라 16일 일몰이 나온다.
			RawAstronomyData night = at(ZonedDateTime.of(2026, 1, 15, 21, 0, 0, 0, SEOUL));

			assertThat(night.sunset()).isEqualTo("17:30");
			assertThat(night.sunrise()).isEqualTo("07:37");
		}

		@Test
		@DisplayName("월출과 월몰도 요청 시각 뒤에 처음 오는 시각이다")
		void moonEventsAreNextOnes() {
			// 15일 정오 기준으로 월몰은 그날 14:00, 15일 밤 기준으로는 다음 날 14:49다.
			RawAstronomyData noon = at(ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, SEOUL));
			RawAstronomyData night = at(ZonedDateTime.of(2026, 1, 15, 21, 0, 0, 0, SEOUL));

			assertThat(noon.moonset()).isEqualTo("14:00");
			assertThat(night.moonset()).isEqualTo("14:49");
			assertThat(night.moonrise()).isEqualTo("05:47");
		}
	}

	private RawAstronomyData at(ZonedDateTime dateTime) {
		return calculator.calculate(LAT, LON, dateTime);
	}
}
