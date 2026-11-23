package xyz.ncookie.stargazer.domain.recommend.dto.request;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "밤 추천 요청. 모든 값이 선택이다")
public record GetNightRecommendationRequest(

	@Schema(description = "출발지 위도. 경도와 함께 보내면 이동 시간을 추정해 순서에 반영한다", example = "37.5665")
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@Schema(description = "출발지 경도", example = "126.9780")
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon,

	@Schema(description = "맑은 시간이 이만큼 이어지면 '꼭 가볼 만해요'로 본다. 기본 3", example = "3")
	@Min(value = 1, message = "맑은 시간 기준은 1시간 이상이어야 합니다.")
	@Max(value = 8, message = "맑은 시간 기준은 8시간 이하이어야 합니다.")
	Integer minClearHours,

	@Schema(description = "이 시각 전에 맑은 시간을 우선한다(HH:mm). 정오 전이면 다음 날 새벽으로 읽는다. 기본 02:00", example = "02:00")
	@DateTimeFormat(pattern = "HH:mm")
	LocalTime preferBefore,

	@Schema(description = "이 시간(분)보다 오래 걸리는 장소는 뺀다. 출발지를 보냈을 때만 쓴다", example = "180")
	@Min(value = 0, message = "이동 시간 한도는 0 이상이어야 합니다.")
	Integer maxTravelMinutes
) {

	@Schema(hidden = true)
	@AssertTrue(message = "출발지는 위도와 경도를 함께 보내야 합니다.")
	public boolean isOriginComplete() {
		return (lat == null) == (lon == null);
	}
}
