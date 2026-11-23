package xyz.ncookie.stargazer.domain.stargazing.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 위상각(-180~180, 0이 보름, ±180이 삭)을 위상 이름으로 바꾸는 경계를 고정한다.
 * 이 규약이 commons-suncalc의 출력과 맞는지는 AstronomyCalculatorTest가 실제 날짜로 확인한다.
 */
class MoonPhaseTest {

	@ParameterizedTest(name = "{0}° → {1}")
	@CsvSource({
		"0, FULL_MOON",
		"9.9, FULL_MOON",
		"-9.9, FULL_MOON",
		"170.1, NEW_MOON",
		"180, NEW_MOON",
		"-170.1, NEW_MOON",
		"-180, NEW_MOON",
	})
	@DisplayName("0°에서 10° 안쪽은 보름달, ±170° 바깥은 삭이다")
	void fullAndNew(double phase, MoonPhase expected) {
		assertThat(MoonPhase.calculate(phase)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{0}° → {1}")
	@CsvSource({
		"10, WANING_GIBBOUS",
		"79.9, WANING_GIBBOUS",
		"80, LAST_QUARTER",
		"99.9, LAST_QUARTER",
		"100, WANING_CRESCENT",
		"170, WANING_CRESCENT",
	})
	@DisplayName("양수는 보름에서 삭으로 기우는 달이다")
	void waning(double phase, MoonPhase expected) {
		assertThat(MoonPhase.calculate(phase)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{0}° → {1}")
	@CsvSource({
		"-170, WAXING_CRESCENT",
		"-100, WAXING_CRESCENT",
		"-99.9, FIRST_QUARTER",
		"-80, FIRST_QUARTER",
		"-79.9, WAXING_GIBBOUS",
		"-10, WAXING_GIBBOUS",
	})
	@DisplayName("음수는 삭에서 보름으로 차오르는 달이다")
	void waxing(double phase, MoonPhase expected) {
		assertThat(MoonPhase.calculate(phase)).isEqualTo(expected);
	}
}
