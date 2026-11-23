package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 기대값은 Krisciunas & Schaefer (1991)의 식 10^(-0.4(0.026|α| + 4×10⁻⁹α⁴))을 손으로 계산한 값이다.
 */
class MoonBrightnessTest {

	@Test
	@DisplayName("보름달은 1이다")
	void fullMoon() {
		assertThat(MoonBrightness.relativeToFull(0)).isEqualTo(1.0);
	}

	@ParameterizedTest(name = "위상각 {0}° → {1}")
	@CsvSource({
		"45.573, 0.330",   // 빛나는 면적 85%
		"90, 0.091",       // 반달. 면적은 절반이지만 밝기는 9%쯤
		"134.427, 0.012",  // 빛나는 면적 15%
		"180, 0.0003",     // 삭
	})
	@DisplayName("위상각이 커질수록 면적보다 훨씬 빠르게 어두워진다")
	void dimsFasterThanArea(double phaseAngle, double expected) {
		assertThat(MoonBrightness.relativeToFull(phaseAngle)).isCloseTo(expected, within(0.0005));
	}

	@Test
	@DisplayName("차오르는 달과 기우는 달은 같은 위상각이면 밝기가 같다")
	void signIsIgnored() {
		assertThat(MoonBrightness.relativeToFull(-90)).isEqualTo(MoonBrightness.relativeToFull(90));
	}

	@Test
	@DisplayName("180°를 넘는 값은 180°로 본다")
	void clampsAboveHalfTurn() {
		assertThat(MoonBrightness.relativeToFull(200)).isEqualTo(MoonBrightness.relativeToFull(180));
	}
}
