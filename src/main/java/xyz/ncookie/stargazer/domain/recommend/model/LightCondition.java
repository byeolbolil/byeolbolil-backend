package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 한 장소가 불빛에 얼마나 둘러싸여 있는지. 날씨와 달리 밤마다 바뀌지 않는 값이다.
 *
 * 세 값은 서로 다른 것을 잰다. 하늘 밝기는 수백 km 안의 불빛이 대기에서 흩어져 하늘 전체를 밝히는 정도이고,
 * 나머지 둘은 불빛이 눈으로 곧바로 들어오는 정도다. 산이 가려 주는 것은 뒤쪽이다.
 *
 * @param skyBrightness  하늘 전체의 인공 밝기가 자연 밤하늘의 몇 배인지(ALR). 자료가 없는 곳이면 null
 * @param nearbyRadiance 둘레 1km 안에서 위로 나가는 빛의 평균(nW/cm²/sr). 자료가 없는 곳이면 null
 * @param lightsInView   1km보다 먼 곳의 불빛 가운데 지형에 가리지 않고 직접 보이는 양의 지수.
 *                       미리 계산해 둔 명소에만 있고 그 밖에는 null
 * @param skyRank        하늘 밝기가 남한 땅에서 몇 번째 칸으로 어두운지(1~100, 1이 가장 어둡다). 평가에는 쓰지 않고
 *                       화면에 보여 주기만 한다. 하늘 밝기를 모르면 null
 */
public record LightCondition(Double skyBrightness, Double nearbyRadiance, Double lightsInView, Integer skyRank) {

	/** 국내 순위를 붙이지 않은 값. 평가는 순위를 보지 않으므로 엔진 테스트는 이것으로 만든다 */
	public LightCondition(Double skyBrightness, Double nearbyRadiance, Double lightsInView) {
		this(skyBrightness, nearbyRadiance, lightsInView, null);
	}

	/** 불빛 자료가 없는 곳 */
	public static final LightCondition UNKNOWN = new LightCondition(null, null, null);

	/** 하늘 밝기만 아는 곳. 테스트와 단순한 계산에 쓴다 */
	public static LightCondition ofSky(double skyBrightness) {
		return new LightCondition(skyBrightness, 0.0, null);
	}

	/** 하늘의 등급. 하늘 밝기를 모르면 null */
	public SkyClass skyClass() {
		return (skyBrightness != null) ? SkyClass.of(skyBrightness) : null;
	}
}
