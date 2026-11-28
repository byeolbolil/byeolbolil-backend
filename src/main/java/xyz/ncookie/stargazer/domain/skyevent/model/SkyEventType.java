package xyz.ncookie.stargazer.domain.skyevent.model;

/**
 * 싣는 천문 현상의 종류. 한국천문연구원의 "주요 천문현상" 가운데 맨눈의 입문자에게 하늘이 달라 보이는 것만 싣는다
 * (docs/decisions.md 2026-11-27 "은하수가 보이는 시간과 천문 현상").
 */
public enum SkyEventType {
	METEOR_SHOWER,
	LUNAR_ECLIPSE
}
