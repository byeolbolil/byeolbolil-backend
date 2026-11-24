package xyz.ncookie.stargazer.domain.geocode.model;

import java.util.List;

/**
 * 지명 검색 결과 하나. 데이터 소스와 무관한 내부 모델이다.
 *
 * @param regions 큰 행정구역부터 작은 순서(도, 군, 시·읍·면, 동·리). 모르는 단계는 빠진다
 */
public record GeocodePlace(
	String name,
	List<String> regions,
	Kind kind,
	double latitude,
	double longitude
) {

	/**
	 * 결과를 늘어놓는 순서를 정하는 데만 쓴다. 선언한 순서가 곧 우선순위다.
	 */
	public enum Kind {
		/** 시, 군, 읍면동 같은 지역과 산, 고개, 천문대처럼 그 자체가 목적지인 곳 */
		PLACE,
		/** 역, 정류장, 나들목, 도로, 학교를 비롯한 나머지 */
		OTHER
	}

	/**
	 * 이름 옆에 붙여 같은 이름의 장소를 구분하는 주소. 예: "경기도 가평군 북면". 모르면 빈 문자열이다.
	 */
	public String address() {
		return String.join(" ", regions.stream().filter(region -> !region.equals(name)).toList());
	}

	/**
	 * 좌표가 어디쯤인지 알려 주는 짧은 이름. 가장 작은 행정구역 둘을 쓴다. 예: "강릉시 왕산면".
	 * 행정구역을 하나도 모르면 장소 이름을 쓴다.
	 */
	public String areaName() {
		if (regions.isEmpty()) {
			return name;
		}
		return String.join(" ", regions.subList(Math.max(0, regions.size() - 2), regions.size()));
	}
}
