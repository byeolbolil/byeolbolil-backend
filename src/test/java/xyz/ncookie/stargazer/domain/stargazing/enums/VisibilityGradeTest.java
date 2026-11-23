package xyz.ncookie.stargazer.domain.stargazing.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 화면에 보여주는 시정 라벨의 경계를 고정한다.
 * 점수 감점은 이 표와 따로 5km, 10km 두 단계만 쓴다(StargazingScoringEngineTest의 시정 항목).
 */
class VisibilityGradeTest {

	@ParameterizedTest(name = "{0}m → {1}")
	@CsvSource({
		"30000, 최상",
		"20000, 최상",
		"19999, 매우 좋음",
		"10000, 매우 좋음",
		"9999, 좋음",
		"5000, 좋음",
		"4999, 보통",
		"2000, 보통",
		"1999, 나쁨",
		"0, 나쁨",
	})
	@DisplayName("20km, 10km, 5km, 2km를 경계로 다섯 단계로 나눈다")
	void labels(int meters, String expectedLabel) {
		assertThat(VisibilityGrade.from(meters).getLabel()).isEqualTo(expectedLabel);
	}
}
