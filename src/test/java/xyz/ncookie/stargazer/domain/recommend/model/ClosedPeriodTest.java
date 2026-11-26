package xyz.ncookie.stargazer.domain.recommend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClosedPeriodTest {

	private static final ClosedPeriod WINTER = new ClosedPeriod(LocalDate.of(2026, 11, 30), LocalDate.of(2027, 4, 1));

	@ParameterizedTest(name = "{0} → 막힘 {1}")
	@CsvSource({
		"2026-11-29, false",
		"2026-11-30, true",
		"2027-01-15, true",
		"2027-04-01, true",
		"2027-04-02, false"
	})
	@DisplayName("첫날과 마지막 날까지가 막히는 기간이다")
	void contains(LocalDate date, boolean expected) {
		assertThat(WINTER.contains(date)).isEqualTo(expected);
	}

	@Test
	@DisplayName("막히는 기간이 없는 장소는 언제나 갈 수 있다")
	void placeWithoutClosure() {
		CandidatePlace place = new CandidatePlace(PlaceType.SPOT, 1L, 1L, "가", "주소", 37.5, 127.0);

		assertThat(place.isClosedOn(LocalDate.of(2027, 1, 15))).isFalse();
	}

	@Test
	@DisplayName("막히는 기간이 있는 장소는 그 기간의 밤에만 갈 수 없다")
	void placeWithClosure() {
		CandidatePlace place = new CandidatePlace(PlaceType.SPOT, 1L, 1L, "가", "주소", 37.5, 127.0, null, null, WINTER);

		assertThat(place.isClosedOn(LocalDate.of(2027, 1, 15))).isTrue();
		assertThat(place.isClosedOn(LocalDate.of(2026, 11, 9))).isFalse();
	}
}
