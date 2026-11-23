package xyz.ncookie.stargazer.domain.recommend.engine;

import xyz.ncookie.stargazer.domain.recommend.model.TravelEstimate;

/**
 * 직선거리로 이동 시간을 어림한다. 길찾기 API를 붙이기 전까지 쓰는 추정이다(로드맵 6단계에서 교체).
 */
public final class TravelEstimator {

	// 지구 평균 반지름 (km)
	private static final double EARTH_RADIUS_KM = 6371.0;

	// 정책(추정): 도로 거리는 직선거리의 1.3배, 평균 시속 60km로 본다. 목업에서 쓴 값이고 근거 자료는 없다.
	// 산간 지역의 명소는 실제로 이보다 오래 걸린다.
	static final double ROAD_DISTANCE_FACTOR = 1.3;
	static final double AVERAGE_SPEED_KMH = 60.0;

	private TravelEstimator() {
	}

	public static TravelEstimate estimate(double fromLat, double fromLon, double toLat, double toLon) {
		double distanceKm = haversineKm(fromLat, fromLon, toLat, toLon);
		int minutes = (int) Math.round(distanceKm * ROAD_DISTANCE_FACTOR / AVERAGE_SPEED_KMH * 60);
		return new TravelEstimate(distanceKm, minutes);
	}

	static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
	}
}
