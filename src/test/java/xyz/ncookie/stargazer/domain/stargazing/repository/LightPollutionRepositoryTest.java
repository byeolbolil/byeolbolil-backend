package xyz.ncookie.stargazer.domain.stargazing.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.ncookie.stargazer.support.LightPollutionFixture.gridPoint;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.ncookie.stargazer.domain.stargazing.entity.LightPollution;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * findNearest가 실제 MySQL에서 어떤 행을 고르는지 고정한다.
 *
 * 쿼리 안에서 좌표를 쓰는 순서가 두 가지다. 범위 상자는 (경도, 위도)로, 정렬 기준점은 WKT 'POINT(위도 경도)'로 만든다.
 * 코드만 읽어서는 결과를 알 수 없어서 docs/issues/scoring.md에 "실행 결과는 확인 못 함"으로 남아 있던 항목이다.
 */
class LightPollutionRepositoryTest extends IntegrationTestSupport {

	// 서울시청 부근
	private static final double LAT = 37.5665;
	private static final double LON = 126.9780;

	@Autowired
	private LightPollutionRepository repository;

	@BeforeEach
	void clearTable() {
		repository.deleteAllInBatch();
	}

	@Test
	@DisplayName("상자 안의 격자점 가운데 가장 가까운 것을 고른다")
	void picksNearestGridPoint() {
		repository.saveAll(List.of(
			gridPoint(LAT + 0.001, LON + 0.001, 9),  // 약 140m
			gridPoint(LAT + 0.005, LON, 7),          // 약 560m
			gridPoint(LAT, LON - 0.008, 5)           // 약 710m
		));

		Optional<LightPollution> nearest = repository.findNearest(LAT, LON);

		assertThat(nearest).map(LightPollution::getBortleClass).contains(9);
	}

	@Test
	@DisplayName("도(°) 차이가 아니라 지표 거리로 비교한다")
	void comparesByGroundDistance() {
		// 위도 37.5°에서 경도 0.001°는 약 88m, 위도 0.001°는 약 111m다.
		// 도 단위로는 첫 번째가 가깝지만 지표 거리로는 두 번째가 가깝다.
		repository.saveAll(List.of(
			gridPoint(LAT + 0.006, LON, 3),  // 0.006°, 약 667m
			gridPoint(LAT, LON + 0.007, 6)   // 0.007°, 약 618m
		));

		Optional<LightPollution> nearest = repository.findNearest(LAT, LON);

		assertThat(nearest).map(LightPollution::getBortleClass).contains(6);
	}

	@Test
	@DisplayName("위도나 경도가 0.01° 넘게 떨어진 격자점은 찾지 못한다")
	void ignoresPointsOutsideSearchBox() {
		repository.saveAll(List.of(
			gridPoint(LAT + 0.011, LON, 2),
			gridPoint(LAT, LON - 0.011, 2)
		));

		assertThat(repository.findNearest(LAT, LON)).isEmpty();
	}

	@Test
	@DisplayName("0.01° 안쪽이면 상자 가장자리 근처의 격자점도 찾는다")
	void findsPointsNearSearchBoxEdge() {
		repository.save(gridPoint(LAT - 0.009, LON + 0.009, 4));

		Optional<LightPollution> nearest = repository.findNearest(LAT, LON);

		assertThat(nearest).map(LightPollution::getBortleClass).contains(4);
	}

	@Test
	@DisplayName("돌려준 격자점의 좌표는 넣은 값 그대로다")
	void returnsStoredCoordinates() {
		repository.save(gridPoint(LAT + 0.002, LON - 0.003, 8));

		LightPollution nearest = repository.findNearest(LAT, LON).orElseThrow();

		assertThat(nearest.getLocation().getY()).isEqualTo(LAT + 0.002);
		assertThat(nearest.getLocation().getX()).isEqualTo(LON - 0.003);
	}
}
