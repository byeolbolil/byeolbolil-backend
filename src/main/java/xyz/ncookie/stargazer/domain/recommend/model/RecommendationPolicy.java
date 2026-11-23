package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.LocalTime;

/**
 * 사용자가 설정에서 바꿀 수 있는 추천 기준. 근거는 docs/SCORING.md "관측 시간대"에 있다.
 *
 * @param minClearHours 맑은 시간이 이만큼 이어지면 "꼭 가볼 만해요"로 본다
 * @param preferBefore  이 시각 전에 맑은 시간을 우선한다. 정오 전이면 다음 날 새벽으로 읽는다
 */
public record RecommendationPolicy(int minClearHours, LocalTime preferBefore) {

	/**
	 * 3시간: 눈이 어둠에 적응하는 40분쯤(Kalloniatis & Luu, Webvision)을 빼고도 2시간 넘게 볼 수 있게 정한 정책값.
	 * 새벽 2시: 입문자가 무리 없이 다녀올 수 있는 시간대로 정한 정책값.
	 */
	public static final RecommendationPolicy DEFAULT = new RecommendationPolicy(3, LocalTime.of(2, 0));

	public RecommendationPolicy {
		if (minClearHours < 1) {
			throw new IllegalArgumentException("minClearHours는 1 이상이어야 합니다.");
		}
		if (preferBefore == null) {
			throw new IllegalArgumentException("preferBefore는 필수입니다.");
		}
	}
}
