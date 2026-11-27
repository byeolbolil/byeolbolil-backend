package xyz.ncookie.stargazer.domain.recommend.application.dto;

import xyz.ncookie.stargazer.domain.recommend.dto.request.GetMoonCalendarRequest;

public record GetMoonCalendarCommand(double latitude, double longitude) {

	public static GetMoonCalendarCommand from(GetMoonCalendarRequest request) {
		return new GetMoonCalendarCommand(request.lat(), request.lon());
	}
}
