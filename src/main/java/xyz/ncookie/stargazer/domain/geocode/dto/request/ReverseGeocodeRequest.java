package xyz.ncookie.stargazer.domain.geocode.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "좌표의 지역 이름 조회 요청")
public record ReverseGeocodeRequest(

	@Schema(description = "위도", example = "37.623", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "위도를 보내야 합니다.")
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@Schema(description = "경도", example = "128.739", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "경도를 보내야 합니다.")
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon
) {
}
