package xyz.ncookie.stargazer.domain.geocode.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "지명 검색 요청")
public record SearchPlaceRequest(

	@Schema(description = "찾을 이름. 2자 이상 50자 이하", example = "화악산", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "검색어를 입력해 주세요.")
	@Size(min = 2, max = 50, message = "검색어는 2자 이상 50자 이하여야 합니다.")
	String q
) {
}
