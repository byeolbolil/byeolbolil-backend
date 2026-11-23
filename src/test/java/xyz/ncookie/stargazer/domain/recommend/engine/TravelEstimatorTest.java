package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.model.TravelEstimate;

class TravelEstimatorTest {

	@Test
	@DisplayName("같은 자리면 거리도 시간도 0이다")
	void samePlace() {
		TravelEstimate travel = TravelEstimator.estimate(37.55, 126.91, 37.55, 126.91);

		assertThat(travel.distanceKm()).isZero();
		assertThat(travel.minutes()).isZero();
	}

	@Test
	@DisplayName("위도 1도는 약 111km다")
	void oneDegreeOfLatitude() {
		assertThat(TravelEstimator.haversineKm(37.0, 127.0, 38.0, 127.0)).isCloseTo(111.2, within(0.1));
	}

	@Test
	@DisplayName("위도 37.5도에서 경도 1도는 약 88km다")
	void oneDegreeOfLongitude() {
		assertThat(TravelEstimator.haversineKm(37.5, 127.0, 37.5, 128.0)).isCloseTo(88.2, within(0.1));
	}

	@Test
	@DisplayName("직선거리의 1.3배를 시속 60km로 간다고 보고 분으로 반올림한다")
	void minutes() {
		// 111.2km × 1.3 = 144.6km, 시속 60km면 144.6분
		TravelEstimate travel = TravelEstimator.estimate(37.0, 127.0, 38.0, 127.0);

		assertThat(travel.minutes()).isEqualTo(145);
	}

	@Test
	@DisplayName("방향을 바꿔도 거리는 같다")
	void symmetric() {
		double there = TravelEstimator.haversineKm(37.55, 126.91, 38.1166, 127.4665);
		double back = TravelEstimator.haversineKm(38.1166, 127.4665, 37.55, 126.91);

		assertThat(there).isCloseTo(back, within(1e-9));
	}
}
