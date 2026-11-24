package xyz.ncookie.stargazer.domain.geocode.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;

@Schema(description = "지명 검색 결과 하나")
public record PlaceSearchResponse(

	@Schema(description = "이름", example = "화악산")
	String name,

	@Schema(description = "같은 이름을 구분하는 주소. 모르면 빈 문자열이다", example = "경기도 가평군 북면")
	String address,

	@Schema(description = "위도", example = "37.9946674")
	double latitude,

	@Schema(description = "경도", example = "127.5034314")
	double longitude
) {

	public static PlaceSearchResponse from(GeocodePlace place) {
		return new PlaceSearchResponse(place.name(), place.address(), place.latitude(), place.longitude());
	}
}
