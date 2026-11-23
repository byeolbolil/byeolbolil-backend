package xyz.ncookie.stargazer.domain.stargazing.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 분석 응답에 나가는 등급별 밝기 문구와 한계등급 문구를 고정한다.
 * 한계등급 값은 출처가 확인되지 않았다(docs/SCORING.md 광해 항목). 지금 표를 그대로 적어 둔 것이다.
 */
class BortleGradeTest {

	@ParameterizedTest(name = "Class {0} → {1}, {2}")
	@CsvSource({
		"1, 매우 어두움, 6.5등급 이상",
		"2, 매우 어두움, 6.5등급",
		"3, 어두움, 6.0등급",
		"4, 어두움, 6.0등급",
		"5, 보통, 5.5등급",
		"6, 보통, 4.5등급",
		"7, 매우 밝음, 4.5등급",
		"8, 매우 밝음, 3.0등급",
		"9, 매우 밝음, 3.0등급",
	})
	@DisplayName("등급마다 밝기 문구와 한계등급 문구가 정해져 있다")
	void texts(int bortle, String brightness, String limitingMag) {
		BortleGrade grade = BortleGrade.from(bortle);

		assertThat(grade.getBrightnessText()).isEqualTo(brightness);
		assertThat(grade.getLimitingMagText()).isEqualTo(limitingMag);
	}

	@ParameterizedTest(name = "{0} → CLASS_9")
	@CsvSource({"0", "10", "-1"})
	@DisplayName("1~9를 벗어난 값은 9등급으로 본다. 엔진이 0을 감점 없이 통과시키는 것과 반대 방향이다")
	void outOfRangeFallsBackToNine(int bortle) {
		assertThat(BortleGrade.from(bortle)).isEqualTo(BortleGrade.CLASS_9);
	}
}
