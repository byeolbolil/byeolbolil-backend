package xyz.ncookie.stargazer.domain.geocode.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "좌표의 지역 이름")
public record ReverseGeocodeResponse(

	@Schema(description = "좌표가 속한 지역. 근처에 아무것도 없으면 빠진다", example = "강릉시 왕산면")
	String name
) {
}
