package xyz.ncookie.stargazer.domain.recommend.application.dto;

import xyz.ncookie.stargazer.domain.recommend.dto.request.GetWeekOutlookRequest;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;

/**
 * @param memberId  내 장소를 볼 때의 회원. 한 위치를 볼 때나 로그인하지 않았을 때는 null
 * @param latitude  한 위치를 볼 때의 위도. 내 장소를 볼 때는 null
 * @param longitude 한 위치를 볼 때의 경도. 내 장소를 볼 때는 null
 */
public record GetWeekOutlookCommand(
	Long memberId,
	Double latitude,
	Double longitude,
	RecommendationPolicy policy
) {

	/** 좌표로 고른 한 위치의 이번 주 */
	public static GetWeekOutlookCommand forLocation(GetWeekOutlookRequest request) {
		return new GetWeekOutlookCommand(null, request.lat(), request.lon(), policyOf(request));
	}

	/** 회원이 저장한 장소들의 이번 주 */
	public static GetWeekOutlookCommand forMember(Long memberId, GetWeekOutlookRequest request) {
		return new GetWeekOutlookCommand(memberId, null, null, policyOf(request));
	}

	private static RecommendationPolicy policyOf(GetWeekOutlookRequest request) {
		RecommendationPolicy defaults = RecommendationPolicy.DEFAULT;
		return new RecommendationPolicy(
			request.minClearHours() != null ? request.minClearHours() : defaults.minClearHours(),
			request.preferBefore() != null ? request.preferBefore() : defaults.preferBefore()
		);
	}
}
