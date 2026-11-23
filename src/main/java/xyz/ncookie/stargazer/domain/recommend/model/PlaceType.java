package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 추천 후보가 어디서 왔는지.
 */
public enum PlaceType {

	/** 서비스가 기본으로 추천하는 명소 */
	SPOT,
	/** 사용자가 저장한 내 장소 */
	BOOKMARK
}
