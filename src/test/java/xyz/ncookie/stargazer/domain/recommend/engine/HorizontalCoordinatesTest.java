package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Meeus, Astronomical Algorithms 2판(1998)의 예제 값으로 확인한다.
 */
class HorizontalCoordinatesTest {

	// 예제 13.b의 관측자: 워싱턴 미 해군 천문대. 책은 서경 77°03′56″를 양수로 적는다
	private static final double USNO_LATITUDE = 38 + 55 / 60.0 + 17 / 3600.0;
	private static final double USNO_LONGITUDE = -(77 + 3 / 60.0 + 56 / 3600.0);

	// 예제 13.b의 금성: α = 23h09m16.641s, δ = -6°43′11.61″
	private static final double VENUS_RA = (23 + 9 / 60.0 + 16.641 / 3600.0) * 15;
	private static final double VENUS_DEC = -(6 + 43 / 60.0 + 11.61 / 3600.0);

	private static final Instant EXAMPLE_13B = Instant.parse("1987-04-10T19:21:00Z");

	@Test
	@DisplayName("예제 12.a: 1987년 4월 10일 0시 UT의 평균 항성시는 13h10m46.3668s다")
	void meanSiderealTimeAtMidnight() {
		double expected = (13 + 10 / 60.0 + 46.3668 / 3600.0) * 15;

		assertThat(HorizontalCoordinates.greenwichMeanSiderealTime(Instant.parse("1987-04-10T00:00:00Z")))
			.isCloseTo(expected, within(1e-5));
	}

	@Test
	@DisplayName("예제 12.b: 1987년 4월 10일 19시 21분 UT의 평균 항성시는 128.7378734°다")
	void meanSiderealTimeAtAnyInstant() {
		assertThat(HorizontalCoordinates.greenwichMeanSiderealTime(EXAMPLE_13B)).isCloseTo(128.7378734, within(1e-5));
	}

	@Test
	@DisplayName("예제 13.b: 금성의 시간각 64.352133°, 고도 15.1249°, 방위각은 남쪽에서 68.0337°다")
	void venusFromWashington() {
		// 책은 장동을 넣은 시태양 항성시를 써서 평균 항성시보다 0.24초(0.004°) 작다. 그만큼 오차를 둔다
		assertThat(HorizontalCoordinates.hourAngle(VENUS_RA, USNO_LONGITUDE, EXAMPLE_13B))
			.isCloseTo(64.352133, within(0.005));
		assertThat(HorizontalCoordinates.altitude(VENUS_RA, VENUS_DEC, USNO_LATITUDE, USNO_LONGITUDE, EXAMPLE_13B))
			.isCloseTo(15.1249, within(0.005));
		assertThat(HorizontalCoordinates.azimuth(VENUS_RA, VENUS_DEC, USNO_LATITUDE, USNO_LONGITUDE, EXAMPLE_13B))
			.isCloseTo(68.0337 + 180, within(0.005));
	}

	@Test
	@DisplayName("기준 고도에 오는 시간각에서 계산한 고도는 그 기준 고도다")
	void hourAngleAtAltitudeIsConsistent() {
		double latitude = 37.5;
		double declination = -29.0;
		double h0 = HorizontalCoordinates.hourAngleAtAltitude(declination, latitude, 10.0);

		double phi = Math.toRadians(latitude);
		double delta = Math.toRadians(declination);
		double altitude = Math.toDegrees(Math.asin(
			Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.cos(Math.toRadians(h0))));

		assertThat(altitude).isCloseTo(10.0, within(1e-9));
	}

	@Test
	@DisplayName("한 번도 기준 고도에 오르지 않으면 0, 내내 그 위에 있으면 180이다")
	void hourAngleAtAltitudeAtTheEnds() {
		// 위도 37.5°에서 적위 -60°인 별은 가장 높을 때도 지평선 아래다
		assertThat(HorizontalCoordinates.hourAngleAtAltitude(-60.0, 37.5, 10.0)).isEqualTo(0.0);
		// 북극성 근처의 별은 위도 37.5°에서 지지 않는다
		assertThat(HorizontalCoordinates.hourAngleAtAltitude(89.0, 37.5, 10.0)).isEqualTo(180.0);
	}
}
