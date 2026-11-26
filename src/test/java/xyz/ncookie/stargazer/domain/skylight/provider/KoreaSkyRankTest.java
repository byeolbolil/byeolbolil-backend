package xyz.ncookie.stargazer.domain.skylight.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 클래스패스의 실제 순위표를 읽는다. 표를 다시 만들면 값이 바뀔 수 있다.
 * 첫 칸의 상한은 1.3122, 둘째 칸은 1.6106, 마지막 칸은 69.5024다.
 */
class KoreaSkyRankTest {

	private final KoreaSkyRank rank = new KoreaSkyRank();

	@Test
	@DisplayName("칸의 상한과 같으면 그 칸이고, 조금이라도 넘으면 다음 칸이다")
	void boundaries() {
		assertThat(rank.rankOf(0.1)).isEqualTo(1);
		assertThat(rank.rankOf(1.3122)).isEqualTo(1);
		assertThat(rank.rankOf(1.3123)).isEqualTo(2);
		assertThat(rank.rankOf(1.6106)).isEqualTo(2);
	}

	@Test
	@DisplayName("가장 밝은 칸의 상한보다 밝으면 100이다")
	void brighterThanAll() {
		assertThat(rank.rankOf(69.5024)).isEqualTo(100);
		assertThat(rank.rankOf(500)).isEqualTo(100);
	}

	@Test
	@DisplayName("영양은 국내에서 가장 어두운 축이고, 안반데기는 상위 22%, 대전 도심은 96번째 칸이다")
	void knownPlaces() {
		assertThat(rank.rankOf(1.49)).isEqualTo(2);
		assertThat(rank.rankOf(3.11)).isEqualTo(22);
		assertThat(rank.rankOf(27.12)).isEqualTo(96);
	}
}
