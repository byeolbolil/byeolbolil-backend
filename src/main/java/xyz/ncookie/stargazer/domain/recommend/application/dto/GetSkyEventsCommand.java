package xyz.ncookie.stargazer.domain.recommend.application.dto;

import xyz.ncookie.stargazer.domain.recommend.dto.request.GetSkyEventsRequest;

/**
 * @param latitude  위치를 모르면 null
 * @param longitude 위치를 모르면 null
 */
public record GetSkyEventsCommand(Double latitude, Double longitude) {

	/** 위도와 경도 가운데 하나만 오면 위치를 모르는 것으로 본다 */
	public static GetSkyEventsCommand from(GetSkyEventsRequest request) {
		if (request.lat() == null || request.lon() == null) {
			return new GetSkyEventsCommand(null, null);
		}
		return new GetSkyEventsCommand(request.lat(), request.lon());
	}

	public boolean hasLocation() {
		return latitude != null && longitude != null;
	}
}
