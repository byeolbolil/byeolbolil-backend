package xyz.ncookie.stargazer.domain.recommend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(description = "다가오는 천문 현상 요청. 위치를 보내면 그 자리에서 보이는 구간과 달을 함께 준다")
public record GetSkyEventsRequest(

	@Schema(description = "위도. lon과 함께 보내야 쓴다", example = "37.6263")
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@Schema(description = "경도. lat과 함께 보내야 쓴다", example = "128.7394")
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon
) {
}
