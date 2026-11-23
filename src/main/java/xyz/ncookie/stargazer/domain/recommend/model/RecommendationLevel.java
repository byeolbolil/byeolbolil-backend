package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 그날 밤 그 장소를 얼마나 권하는지. 화면에서는 별 0~3개로 보여준다.
 * 선언 순서가 낮은 등급에서 높은 등급 순이다.
 */
public enum RecommendationLevel {

	/** 오늘은 쉬어가요 */
	NO,
	/** 잠깐 볼 수 있어요 */
	MAYBE,
	/** 가볼 만해요 */
	GOOD,
	/** 꼭 가볼 만해요 */
	STRONG;

	/** 한 단계 낮춘다. 맑은 시간이 있는 밤은 MAYBE 아래로 내리지 않는다. */
	public RecommendationLevel loweredOnce() {
		return (this.ordinal() <= MAYBE.ordinal()) ? this : values()[this.ordinal() - 1];
	}

	public boolean isAbove(RecommendationLevel other) {
		return this.ordinal() > other.ordinal();
	}
}
