package xyz.ncookie.stargazer.support;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import xyz.ncookie.stargazer.domain.stargazing.entity.LightPollution;

/**
 * 테스트용 광해 격자점을 만든다.
 *
 * 좌표를 넣는 방식은 LightPollutionMigrationRunner와 같다(SRID 4326, x=경도, y=위도).
 * 러너의 적재 방식이 바뀌면 여기도 같이 바꿔야 조회 테스트가 실제 적재 데이터와 같은 조건이 된다.
 */
public final class LightPollutionFixture {

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	private LightPollutionFixture() {
	}

	public static LightPollution gridPoint(double lat, double lon, int bortleClass) {
		return new LightPollution(
			GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat)),
			bortleClass,
			0.0
		);
	}
}
