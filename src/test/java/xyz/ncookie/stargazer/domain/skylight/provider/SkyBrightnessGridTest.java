package xyz.ncookie.stargazer.domain.skylight.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import xyz.ncookie.stargazer.domain.skylight.model.SkyLight;

class SkyBrightnessGridTest {

	@Nested
	@DisplayName("좌표로 값 찾기")
	class Lookup {

		// 세 줄 세 칸짜리 격자. 북쪽 끝 38°, 서쪽 끝 127°, 간격 1°라서 칸의 중심은 위도 37.5, 36.5, 35.5와
		// 경도 127.5, 128.5, 129.5다. 하늘 밝기는 위 두 줄이 서쪽부터 1, 10, 100이고 맨 아랫줄은 모두 10이다.
		// 둘레 불빛은 북서쪽 칸만 100이고 나머지는 1이다.
		private final SkyBrightnessGrid grid = new SkyBrightnessGrid(new ClassPathResource("skyglow/test-grid.bin.gz"));

		@Test
		@DisplayName("칸의 중심에서는 그 칸의 값을 그대로 준다")
		void cellCentre() {
			SkyLight light = grid.lookup(37.5, 128.5).orElseThrow();

			assertThat(light.skyBrightness()).isCloseTo(10.0, within(1e-9));
			assertThat(light.nearbyRadiance()).isCloseTo(1.0, within(1e-9));
		}

		@Test
		@DisplayName("두 칸 사이에서는 로그로 보간한다. 1과 100의 한가운데는 10이다")
		void betweenCells() {
			assertThat(grid.lookup(37.5, 128.0).orElseThrow().skyBrightness()).isCloseTo(Math.sqrt(10), within(1e-9));
			assertThat(grid.lookup(37.0, 127.5).orElseThrow().skyBrightness()).isCloseTo(1.0, within(1e-9));
			assertThat(grid.lookup(36.0, 127.5).orElseThrow().skyBrightness()).isCloseTo(Math.sqrt(10), within(1e-9));
			assertThat(grid.lookup(37.5, 128.0).orElseThrow().nearbyRadiance()).isCloseTo(10.0, within(1e-9));
		}

		@Test
		@DisplayName("가장자리 칸의 중심까지가 범위다")
		void edges() {
			assertThat(grid.lookup(37.5, 127.5)).isPresent();
			assertThat(grid.lookup(35.5, 129.5)).isPresent();
			assertThat(grid.lookup(35.5, 129.5).orElseThrow().skyBrightness()).isCloseTo(10.0, within(1e-9));
		}

		@Test
		@DisplayName("범위 밖의 좌표는 값이 없다")
		void outside() {
			assertThat(grid.lookup(37.6, 128.5)).isEmpty();
			assertThat(grid.lookup(35.4, 128.5)).isEmpty();
			assertThat(grid.lookup(36.5, 127.4)).isEmpty();
			assertThat(grid.lookup(36.5, 129.6)).isEmpty();
		}
	}

	/**
	 * 앱에 실린 격자의 값을 고정한다. scripts/lightpollution으로 격자를 다시 만들면 값이 조금 달라질 수 있으므로
	 * 범위로 본다. 범위를 벗어나면 격자가 잘못 만들어졌거나 모형이 바뀐 것이다.
	 */
	@Nested
	@DisplayName("한국 격자")
	class KoreaGrid {

		private final SkyBrightnessGrid grid = new SkyBrightnessGrid();

		@Test
		@DisplayName("서울 한복판은 하늘도 둘레도 밝다")
		void seoul() {
			SkyLight light = grid.lookup(37.5663, 126.9779).orElseThrow();

			assertThat(light.skyBrightness()).isBetween(55.0, 85.0);
			assertThat(light.nearbyRadiance()).isBetween(70.0, 130.0);
		}

		@Test
		@DisplayName("영양 반딧불이생태공원은 후보지 가운데 가장 어둡다")
		void yeongyang() {
			SkyLight light = grid.lookup(36.83047, 129.26906).orElseThrow();

			assertThat(light.skyBrightness()).isBetween(1.2, 1.9);
			assertThat(light.nearbyRadiance()).isLessThan(1.0);
		}

		@Test
		@DisplayName("부여 성흥산성은 제천 덕주산성보다 하늘이 밝다")
		void brighterNearThePlain() {
			double seongheung = grid.lookup(36.1961432, 126.9002635).orElseThrow().skyBrightness();
			double deokju = grid.lookup(36.86206, 128.09643).orElseThrow().skyBrightness();

			assertThat(seongheung).isBetween(5.0, 7.0);
			assertThat(deokju).isBetween(3.4, 4.6);
		}

		@Test
		@DisplayName("작은 도시의 시내는 하늘 밝기로는 걸러지지 않아도 둘레 불빛으로 걸러진다")
		void smallCityCentre() {
			SkyLight gangneung = grid.lookup(37.752, 128.896).orElseThrow();

			assertThat(gangneung.skyBrightness()).isBetween(8.0, 12.0);
			assertThat(gangneung.nearbyRadiance()).isGreaterThan(40.0);
		}

		@Test
		@DisplayName("한국 둘레 밖은 자료가 없다")
		void outsideKorea() {
			Optional<SkyLight> tokyo = grid.lookup(35.6812, 139.7671);

			assertThat(tokyo).isEmpty();
		}
	}
}
