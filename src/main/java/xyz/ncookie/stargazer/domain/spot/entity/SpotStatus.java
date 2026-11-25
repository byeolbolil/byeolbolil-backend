package xyz.ncookie.stargazer.domain.spot.entity;

/**
 * 명소를 지금 보여 주는지. 뺀 곳을 지우지 않고 RETIRED로 두는 것은 그 명소를 저장해 둔 북마크가 있을 수 있어서다.
 */
public enum SpotStatus {
	ACTIVE,
	RETIRED
}
