package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 밤사이 하늘이 어떻게 바뀌는지. 한 시각의 점수로는 드러나지 않는 차이를 구분한다.
 */
public enum NightPattern {

	/** 밤새 맑아요 */
	CLEAR_ALL_NIGHT,
	/** 초저녁이 가장 맑아요 */
	CLEAR_EARLY,
	/** 늦은 밤에 개요 */
	CLEARS_LATE,
	/** 흐렸다가 다시 개요 */
	CLEARS_AGAIN,
	/** 잠깐 맑은 틈이 있어요. 위 넷 어디에도 들지 않는 경우다 */
	BRIEF_CLEAR,
	/** 밤새 흐려요 */
	CLOUDY_ALL_NIGHT
}
