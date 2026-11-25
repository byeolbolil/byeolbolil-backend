package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 등급만으로는 전해지지 않는 주의 사항.
 */
public enum NightNote {

	/** 추천 시간이 모두 선호 시각 뒤에 있다. 늦게 맑아진다 */
	CLEARS_LATE,
	/** 달이 밝아서 등급을 한 단계 내렸다 */
	BRIGHT_MOON,
	/** 도시 불빛이 강해서 등급을 낮췄다 */
	CITY_LIGHTS,
	/** 아래쪽 마을이나 도시의 불빛이 지형에 가리지 않고 직접 보인다. 등급은 그대로 두고 알리기만 한다 */
	LIGHTS_IN_VIEW,
	/** 추천 시간에 습도가 높아 장비에 이슬이 맺힐 수 있다 */
	DEW
}
