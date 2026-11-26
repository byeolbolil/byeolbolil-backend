package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Criteria;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;

@Schema(description = "오늘 밤부터 7일의 밤마다 후보 가운데 가장 좋은 등급과 별 보기 좋은 곳의 수")
public record NightSummaryResponse(

	@Schema(description = "오늘 밤의 날짜. 새벽 4시 전에는 전날이다", example = "2026-11-25")
	String startDate,

	@Schema(description = "평가에 쓴 기준")
	Criteria criteria,

	@Schema(description = "startDate부터 7일의 밤. 날짜순이고 항상 7개다")
	List<Night> nights
) {

	/**
	 * @param bestLevels 밤마다 가장 좋은 등급. 평가한 장소가 없는 밤은 들어 있지 않다
	 * @param goodCounts 밤마다 별 보기 좋은 곳(GOOD 이상)의 수. 평가한 장소가 없는 밤은 들어 있지 않다
	 */
	public static NightSummaryResponse of(
		LocalDate startDate,
		int days,
		RecommendationPolicy policy,
		Integer maxTravelMinutes,
		Map<LocalDate, RecommendationLevel> bestLevels,
		Map<LocalDate, Integer> goodCounts
	) {
		return new NightSummaryResponse(
			startDate.toString(),
			Criteria.of(policy, maxTravelMinutes),
			startDate.datesUntil(startDate.plusDays(days))
				.map(date -> Night.of(date, bestLevels.get(date), goodCounts.get(date)))
				.toList()
		);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "하룻밤의 요약")
	public record Night(
		@Schema(description = "밤이 시작하는 날짜", example = "2026-11-25")
		String date,
		@Schema(description = "그 밤의 후보 가운데 가장 좋은 등급. STRONG, GOOD, MAYBE, NO. "
			+ "같은 조건으로 부른 날짜별 밤 추천의 첫 장소 등급과 같다. 평가한 장소가 없으면 빠진다")
		String level,
		@Schema(description = "그 밤에 별 보기 좋은 곳(STRONG, GOOD)의 수. 같은 조건으로 부른 날짜별 밤 추천에서 "
			+ "그 등급인 장소의 수와 같다. 평가한 장소가 없으면 빠진다", example = "8")
		Integer goodCount
	) {
		static Night of(LocalDate date, RecommendationLevel level, Integer goodCount) {
			return new Night(date.toString(), level != null ? level.name() : null, goodCount);
		}
	}
}
