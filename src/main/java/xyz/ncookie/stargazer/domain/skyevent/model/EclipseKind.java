package xyz.ncookie.stargazer.domain.skyevent.model;

/**
 * 월식의 종류. 달이 지구의 본그림자에 모두 들어가면 개기, 일부만 들어가면 부분, 반그림자에만 들어가면 반영 월식이다.
 */
public enum EclipseKind {
	TOTAL,
	PARTIAL,
	PENUMBRAL
}
