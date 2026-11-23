package xyz.ncookie.stargazer.domain.weather.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ForecastCellTest {

	@Test
	@DisplayName("좌표를 0.05° 격자에서 가장 가까운 칸으로 맞춘다")
	void snapsToNearestCell() {
		// 강릉 안반데기
		ForecastCell cell = ForecastCell.of(37.6254, 128.9392);

		assertThat(cell.latitude()).isEqualTo(37.65);
		assertThat(cell.longitude()).isEqualTo(128.95);
	}

	@ParameterizedTest(name = "({0}, {1}) → {2}")
	@CsvSource({
		"37.6254, 128.9392, 753:2579",
		"37.66, 128.94, 753:2579",       // 같은 칸
		"37.6251, 128.9251, 753:2579",   // 칸의 남서쪽 끝
		"37.6249, 128.9392, 752:2579",   // 위도가 경계를 넘으면 아래 칸
		"37.6254, 128.9249, 753:2578",   // 경도가 경계를 넘으면 왼쪽 칸
	})
	@DisplayName("칸의 경계는 0.05°의 배수에서 ±0.025° 떨어진 곳이다")
	void cellBoundaries(double latitude, double longitude, String expectedKey) {
		assertThat(ForecastCell.of(latitude, longitude).key()).isEqualTo(expectedKey);
	}

	@Test
	@DisplayName("남반구와 서경에서도 가장 가까운 칸으로 맞춘다")
	void negativeCoordinates() {
		ForecastCell cell = ForecastCell.of(-33.8688, -70.6693);

		assertThat(cell.latitude()).isEqualTo(-33.85);
		assertThat(cell.longitude()).isEqualTo(-70.65);
		assertThat(cell.key()).isEqualTo("-677:-1413");
	}

	@Test
	@DisplayName("외부 API에는 칸 중심 좌표를 소수 둘째 자리까지 보낸다")
	void requestParams() {
		ForecastCell cell = ForecastCell.of(37.6254, 128.9392);

		assertThat(cell.latitudeParam()).isEqualTo("37.65");
		assertThat(cell.longitudeParam()).isEqualTo("128.95");
	}

	@Test
	@DisplayName("정수 위도의 칸도 둘째 자리까지 적는다")
	void requestParamsKeepTwoDecimals() {
		ForecastCell cell = ForecastCell.of(38.0, 127.0);

		assertThat(cell.latitudeParam()).isEqualTo("38.00");
		assertThat(cell.longitudeParam()).isEqualTo("127.00");
	}
}
