package xyz.ncookie.stargazer.domain.recommend.dto.request;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "이번 주 밤 요청. 좌표는 한 위치를 볼 때만 보낸다")
public record GetWeekOutlookRequest(

	@Schema(description = "볼 위치의 위도. 경도와 함께 보낸다", example = "37.6263")
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@Schema(description = "볼 위치의 경도", example = "128.7394")
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon,

	@Schema(description = "맑은 시간이 이만큼 이어지면 '별 보기 아주 좋아요'로 본다. 기본 3", example = "3")
	@Min(value = 1, message = "맑은 시간 기준은 1시간 이상이어야 합니다.")
	@Max(value = 8, message = "맑은 시간 기준은 8시간 이하이어야 합니다.")
	Integer minClearHours,

	@Schema(description = "이 시각 전에 맑은 시간을 우선한다(HH:mm). 정오 전이면 다음 날 새벽으로 읽는다. 기본 02:00", example = "02:00")
	@DateTimeFormat(pattern = "HH:mm")
	LocalTime preferBefore
) {

	@Schema(hidden = true)
	@AssertTrue(message = "위치는 위도와 경도를 함께 보내야 합니다.")
	public boolean isLocationComplete() {
		return (lat == null) == (lon == null);
	}
}
