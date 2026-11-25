package xyz.ncookie.stargazer.domain.spot.entity;

/**
 * 명소의 종류. 밤 추천은 OPEN_SITE만 후보로 삼는다.
 * 나눈 이유와 조사 기록은 docs/decisions.md "명소 목록 재선정"과 docs/issues/spots.md에 있다.
 */
public enum SpotKind {

	/** 누구나 밤에 가서 맨눈으로 볼 수 있는 곳. 고개, 주차장, 전망대, 공원, 해변 */
	OPEN_SITE,

	/** 운영 시간에 프로그램으로 망원경을 보는 천문대와 과학관 */
	FACILITY
}
