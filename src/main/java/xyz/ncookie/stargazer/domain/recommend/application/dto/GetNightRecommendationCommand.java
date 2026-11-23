package xyz.ncookie.stargazer.domain.recommend.application.dto;

import java.time.LocalDate;

import xyz.ncookie.stargazer.domain.recommend.dto.request.GetNightRecommendationRequest;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;

/**
 * @param memberId         로그인하지 않았으면 null. 그때는 기본 추천 명소만 본다
 * @param date             밤이 시작하는 날짜. null이면 오늘 밤
 * @param originLatitude   출발지 위도. 없으면 null
 * @param originLongitude  출발지 경도. 없으면 null
 * @param maxTravelMinutes 이동 시간 한도. 없으면 null
 */
public record GetNightRecommendationCommand(
	Long memberId,
	LocalDate date,
	Double originLatitude,
	Double originLongitude,
	RecommendationPolicy policy,
	Integer maxTravelMinutes
) {

	public static GetNightRecommendationCommand from(Long memberId, LocalDate date, GetNightRecommendationRequest request) {
		RecommendationPolicy defaults = RecommendationPolicy.DEFAULT;
		RecommendationPolicy policy = new RecommendationPolicy(
			request.minClearHours() != null ? request.minClearHours() : defaults.minClearHours(),
			request.preferBefore() != null ? request.preferBefore() : defaults.preferBefore()
		);

		return new GetNightRecommendationCommand(
			memberId,
			date,
			request.lat(),
			request.lon(),
			policy,
			request.maxTravelMinutes()
		);
	}

	public boolean hasOrigin() {
		return originLatitude != null && originLongitude != null;
	}
}
