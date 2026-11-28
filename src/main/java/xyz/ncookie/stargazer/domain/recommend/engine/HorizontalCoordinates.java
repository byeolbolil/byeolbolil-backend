package xyz.ncookie.stargazer.domain.recommend.engine;

import java.time.Duration;
import java.time.Instant;

/**
 * 적경과 적위로 주어진 천체가 어느 시각, 어느 자리에서 하늘의 어디에 있는지 계산한다.
 * commons-suncalc는 해와 달만 계산하므로, 은하수 중심이나 유성우의 복사점 같은 고정된 점은 여기서 계산한다.
 *
 * 식은 Meeus, Astronomical Algorithms 2판(1998)의 12장(항성시)과 13장(좌표 변환)을 따른다.
 * 경도는 동경을 양수로 받는다. Meeus는 서경을 양수로 쓰므로 식에서 부호가 반대다.
 * 대기 굴절은 넣지 않는다. 고도 10°에서 굴절은 0.1°쯤이라(Meeus 16장) 구간의 끝이 1분 안쪽으로 달라진다.
 */
public final class HorizontalCoordinates {

	/** 2000년 1월 1일 12시의 율리우스일. 식 12.4가 시간을 이 시각부터 센다 */
	static final double J2000_JULIAN_DAY = 2451545.0;

	/** 1970-01-01T00:00Z의 율리우스일. Meeus 7장의 방법으로 셈한 값이다 */
	private static final double UNIX_EPOCH_JULIAN_DAY = 2440587.5;

	private static final double SECONDS_PER_DAY = 86400.0;

	/**
	 * 평균 항성시가 하루(평균 태양일)에 도는 각(°). Meeus 식 12.4의 1차항 계수다.
	 * 이 값이 하루 360°보다 큰 만큼 별은 날마다 3분 56초쯤 일찍 뜬다.
	 */
	static final double SIDEREAL_DEGREES_PER_DAY = 360.98564736629;

	private HorizontalCoordinates() {
	}

	/** 시각의 율리우스일. 식 12.4는 세계시(UT1)로 쓴다. UTC와 UT1은 0.9초 안쪽으로 맞춰져 있어 구분하지 않는다 */
	static double julianDay(Instant time) {
		return UNIX_EPOCH_JULIAN_DAY + (time.getEpochSecond() + time.getNano() / 1e9) / SECONDS_PER_DAY;
	}

	/**
	 * 그리니치 평균 항성시(°, 0 이상 360 미만). Meeus 식 12.4.
	 * 장동을 넣은 시태양 항성시와의 차이는 1초 안쪽이라 넣지 않는다.
	 */
	public static double greenwichMeanSiderealTime(Instant time) {
		double days = julianDay(time) - J2000_JULIAN_DAY;
		double t = days / 36525.0;
		double theta = 280.46061837
			+ SIDEREAL_DEGREES_PER_DAY * days
			+ 0.000387933 * t * t
			- t * t * t / 38710000.0;
		return normalize(theta);
	}

	/**
	 * 시간각(°, -180 초과 180 이하). 천체가 자오선을 지난 뒤 얼마나 서쪽으로 돌았는지다. Meeus 13장, H = θ0 - L - α.
	 *
	 * @param longitude 동경이 양수
	 */
	public static double hourAngle(double rightAscension, double longitude, Instant time) {
		double h = normalize(greenwichMeanSiderealTime(time) + longitude - rightAscension);
		return (h > 180.0) ? h - 360.0 : h;
	}

	/** 고도(°). Meeus 식 13.6 */
	public static double altitude(double rightAscension, double declination, double latitude, double longitude, Instant time) {
		double h = Math.toRadians(hourAngle(rightAscension, longitude, time));
		double phi = Math.toRadians(latitude);
		double delta = Math.toRadians(declination);
		return Math.toDegrees(Math.asin(
			Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.cos(h)));
	}

	/**
	 * 방위각(°). 북쪽이 0°이고 동쪽으로 돈다. Meeus 식 13.5는 남쪽에서 서쪽으로 재므로 180°를 더한다.
	 */
	public static double azimuth(double rightAscension, double declination, double latitude, double longitude, Instant time) {
		double h = Math.toRadians(hourAngle(rightAscension, longitude, time));
		double phi = Math.toRadians(latitude);
		double delta = Math.toRadians(declination);
		double fromSouth = Math.toDegrees(Math.atan2(
			Math.sin(h), Math.cos(h) * Math.sin(phi) - Math.tan(delta) * Math.cos(phi)));
		return normalize(fromSouth + 180.0);
	}

	/**
	 * 천체가 고도 altitude에 오는 시간각의 크기(°). 시간각이 ±이 값 안에 있으면 그보다 높이 있다.
	 * Meeus 식 15.1을 기준 고도 h0에 대해 푼 것이다.
	 *
	 * @return 하루 내내 그보다 높으면 180, 한 번도 그만큼 오르지 않으면 0
	 */
	static double hourAngleAtAltitude(double declination, double latitude, double altitude) {
		double phi = Math.toRadians(latitude);
		double delta = Math.toRadians(declination);
		double cosH = (Math.sin(Math.toRadians(altitude)) - Math.sin(phi) * Math.sin(delta))
			/ (Math.cos(phi) * Math.cos(delta));
		if (cosH <= -1.0) {
			return 180.0;
		}
		if (cosH >= 1.0) {
			return 0.0;
		}
		return Math.toDegrees(Math.acos(cosH));
	}

	/** 항성시로 시간각이 degrees만큼 도는 데 걸리는 시간 */
	static Duration siderealDuration(double degrees) {
		return Duration.ofNanos(Math.round(degrees / SIDEREAL_DEGREES_PER_DAY * SECONDS_PER_DAY * 1e9));
	}

	private static double normalize(double degrees) {
		double d = degrees % 360.0;
		return (d < 0) ? d + 360.0 : d;
	}
}
