package xyz.ncookie.stargazer.domain.weather.model;

/**
 * 구름 지도를 그리려고 예보를 받는 격자. 남서쪽 모서리에서 시작해 같은 간격으로 북쪽과 동쪽으로 간다.
 *
 * 좌표는 0.1° 단위로만 정한다. 요청 주소에 소수 한 자리로 적기 때문이다.
 *
 * @param south   가장 남쪽 줄의 위도
 * @param west    가장 서쪽 칸의 경도
 * @param step    줄과 칸 사이의 간격(도)
 * @param rows    남쪽에서 북쪽으로 줄의 수
 * @param columns 서쪽에서 동쪽으로 칸의 수
 */
public record CloudGridSpec(double south, double west, double step, int rows, int columns) {

	/**
	 * 한국과 그 주변 바다. 위도 32.6°~39.0°, 경도 124.2°~131.0°를 0.4° 간격으로 나눈 17줄 18칸(306점)이다.
	 *
	 * 간격은 Open-Meteo 무료 API의 한도에서 정했다. 여러 지점을 한 번에 요청하면 지점 수만큼 호출로 세고,
	 * 한도는 분당 600회, 하루 10,000회다(https://open-meteo.com/en/terms). 모델이 하루 네 번 새로 돌 때마다 받아도
	 * 하루 1,200회 남짓이고, 같은 때에 명소 예보를 받는 호출과 합쳐도 분당 한도를 넘지 않는다.
	 * 0.25°로 나누면 648점이라 한 번에 받을 수 없다.
	 */
	public static final CloudGridSpec KOREA = new CloudGridSpec(32.6, 124.2, 0.4, 17, 18);

	public int size() {
		return rows * columns;
	}

	public double latitudeAt(int row) {
		return roundToTenth(south + step * row);
	}

	public double longitudeAt(int column) {
		return roundToTenth(west + step * column);
	}

	private static double roundToTenth(double value) {
		return Math.round(value * 10.0) / 10.0;
	}
}
