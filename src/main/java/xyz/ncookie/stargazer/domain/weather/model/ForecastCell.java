package xyz.ncookie.stargazer.domain.weather.model;

import java.util.Locale;

/**
 * 예보를 요청하고 캐시하는 단위.
 *
 * 사용자 좌표를 0.05° 격자에 맞춘다. 같은 칸에 들어오는 좌표는 한 번 받은 예보를 같이 쓴다.
 * 주 모델인 ECMWF IFS의 격자가 약 9km(0.08° 안팎)라서 그보다 촘촘한 0.05°(위도 방향 약 5.5km)로 잡았다.
 * 칸을 모델 격자보다 크게 잡으면 한 칸 안에서 서로 다른 모델 격자점을 써야 할 좌표가 섞인다.
 * 모델 격자는 Open-Meteo의 모델 메타 응답(crs_wkt "Reduced Gaussian Grid O1280")으로 확인했다.
 *
 * @param latIndex 위도 × 20을 반올림한 값
 * @param lonIndex 경도 × 20을 반올림한 값
 */
public record ForecastCell(int latIndex, int lonIndex) {

	private static final int CELLS_PER_DEGREE = 20;

	public static ForecastCell of(double latitude, double longitude) {
		return new ForecastCell(
			(int) Math.round(latitude * CELLS_PER_DEGREE),
			(int) Math.round(longitude * CELLS_PER_DEGREE)
		);
	}

	/** 칸 중심의 위도 */
	public double latitude() {
		return (double) latIndex / CELLS_PER_DEGREE;
	}

	/** 칸 중심의 경도 */
	public double longitude() {
		return (double) lonIndex / CELLS_PER_DEGREE;
	}

	/** 외부 API에 보낼 좌표 문자열. 소수 둘째 자리까지면 칸 중심을 정확히 나타낸다. */
	public String latitudeParam() {
		return String.format(Locale.ROOT, "%.2f", latitude());
	}

	public String longitudeParam() {
		return String.format(Locale.ROOT, "%.2f", longitude());
	}

	/** 캐시 키에 쓰는 표현 */
	public String key() {
		return latIndex + ":" + lonIndex;
	}
}
