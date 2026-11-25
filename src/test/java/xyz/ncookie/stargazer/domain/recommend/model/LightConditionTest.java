package xyz.ncookie.stargazer.domain.recommend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LightConditionTest {

	@ParameterizedTest(name = "하늘 밝기 {0} → {1}")
	@CsvSource({
		"0, NATURAL", "0.32, NATURAL",
		"0.33, DARK", "1.99, DARK",
		"2, RURAL", "9.99, RURAL",
		"10, SUBURBAN", "49.9, SUBURBAN",
		"50, URBAN", "500, URBAN"
	})
	@DisplayName("등급의 경계는 0.33, 2, 10, 50이고 경계값은 더 밝은 쪽 등급이다")
	void skyClassBoundaries(double skyBrightness, SkyClass expected) {
		assertThat(SkyClass.of(skyBrightness)).isEqualTo(expected);
		assertThat(LightCondition.ofSky(skyBrightness).skyClass()).isEqualTo(expected);
	}

	@Test
	@DisplayName("하늘 밝기를 모르면 등급도 없다")
	void unknown() {
		assertThat(LightCondition.UNKNOWN.skyClass()).isNull();
	}
}
