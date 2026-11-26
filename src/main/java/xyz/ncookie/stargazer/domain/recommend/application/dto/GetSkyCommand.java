package xyz.ncookie.stargazer.domain.recommend.application.dto;

import xyz.ncookie.stargazer.domain.recommend.dto.request.GetSkyRequest;

public record GetSkyCommand(double latitude, double longitude) {

	public static GetSkyCommand from(GetSkyRequest request) {
		return new GetSkyCommand(request.lat(), request.lon());
	}
}
