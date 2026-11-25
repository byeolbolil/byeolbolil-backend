package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 하늘 밝기(ALR)로 나눈 하늘의 등급. 화면에 붙이는 이름표이고 점수 계산에는 연속값을 쓴다.
 *
 * 경계 0.33, 2, 10은 Duriscoe et al., "A simplified model of all-sky artificial sky glow derived from VIIRS
 * Day/Night band data", JQSRT 214, 133 (2018)의 표 3이다. 미국 국립공원관리청의 기준(0.33 미만 좋음, 2까지 보통,
 * 그 위는 나쁨)에 은하수가 보이지 않게 되는 경계 10을 더한 표다.
 * 경계 50은 Falchi et al., Science Advances 2, e1600377 (2016)이 "눈이 어둠에 적응하지 못한다"고 한 천정 밝기
 * 3000 µcd/m²(자연의 17.2배)를 ALR로 옮긴 값이다. 옮길 때는 Duriscoe 논문 표 2의 10km 줄과 20km 줄을 로그로
 * 보간했다. 표 2가 도시 하나를 모형으로 계산한 값이라 이 경계는 어림값이다.
 * 등급의 이름은 별볼일이 붙였다.
 */
public enum SkyClass {

	/** 자연 상태와 거의 같은 하늘 */
	NATURAL(0.33),
	/** 지평선 쪽에만 불빛이 비치는 어두운 하늘 */
	DARK(2.0),
	/** 은하수는 보이지만 불빛이 하늘 여러 곳에 번진 하늘 */
	RURAL(10.0),
	/** 은하수가 보이지 않는 하늘 */
	SUBURBAN(50.0),
	/** 눈이 어둠에 적응하지 못할 만큼 밝은 하늘 */
	URBAN(Double.POSITIVE_INFINITY);

	private final double upperBound;

	SkyClass(double upperBound) {
		this.upperBound = upperBound;
	}

	/** 이 등급의 위쪽 경계(ALR). 이 값부터는 다음 등급이다 */
	public double upperBound() {
		return upperBound;
	}

	public static SkyClass of(double skyBrightness) {
		for (SkyClass skyClass : values()) {
			if (skyBrightness < skyClass.upperBound) {
				return skyClass;
			}
		}
		return URBAN;
	}
}
