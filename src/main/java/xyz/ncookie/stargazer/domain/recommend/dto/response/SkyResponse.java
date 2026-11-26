package xyz.ncookie.stargazer.domain.recommend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.engine.NightAssessmentEngine;
import xyz.ncookie.stargazer.domain.recommend.model.LightCondition;

@Schema(description = "한 좌표의 하늘 밝기. 예보는 보지 않는다")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkyResponse(

	@Schema(description = "요청한 위도")
	double latitude,

	@Schema(description = "요청한 경도")
	double longitude,

	@Schema(description = "하늘 전체에서 평균한 인공 불빛이 자연 밤하늘 밝기의 몇 배인지(ALR). 자료가 있는 범위(한국 둘레) 밖이면 빠진다",
		example = "3.17")
	Double skyBrightness,

	@Schema(description = "하늘 밝기 등급. NATURAL, DARK, RURAL, SUBURBAN, URBAN 가운데 하나. 범위 밖이면 빠진다",
		example = "RURAL")
	String skyClass,

	@Schema(description = "하늘 밝기가 남한 땅에서 몇 번째 칸으로 어두운지(1~100). 1이면 가장 어두운 1% 안이다. 자료가 없는 곳이면 빠진다", example = "21")
	Integer skyRank,

	@Schema(description = "둘레 1km가 가로등과 건물 불빛으로 밝은지. 밝으면 밤 추천에서 맑아도 '별 보기 아쉬워요'까지만 준다. "
		+ "범위 밖이면 빠진다", example = "false")
	Boolean litSurroundings
) {

	public static SkyResponse of(double latitude, double longitude, LightCondition light) {
		Boolean lit = (light.nearbyRadiance() != null) ? NightAssessmentEngine.hasLitSurroundings(light) : null;
		return new SkyResponse(latitude, longitude, SkyValues.brightness(light), SkyValues.className(light),
			SkyValues.rank(light), lit);
	}
}
