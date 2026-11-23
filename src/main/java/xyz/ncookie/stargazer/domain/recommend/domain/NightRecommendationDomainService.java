package xyz.ncookie.stargazer.domain.recommend.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.engine.NightAssessmentEngine;
import xyz.ncookie.stargazer.domain.recommend.engine.TravelEstimator;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.recommend.model.TravelEstimate;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * 후보 장소 하나를 평가하고, 평가한 장소들의 순서를 정한다.
 */
@Service
@RequiredArgsConstructor
public class NightRecommendationDomainService {

	// 정책: 이동 1시간마다 추천 구간의 질에서 4점을 뺀다. 목업에서 쓴 값이고 근거 자료는 없다.
	// 등급이 같은 장소 사이에서만 영향을 준다. 가깝다는 이유로 등급이 낮은 곳이 위로 올라오지는 않는다.
	static final double TRAVEL_PENALTY_PER_HOUR = 4.0;

	private final NightAssessmentEngine nightAssessmentEngine;

	/**
	 * @param originLatitude  출발지 위도. 모르면 null
	 * @param originLongitude 출발지 경도. 모르면 null
	 * @return 그 밤에 평가할 예보가 없으면 비어 있다
	 */
	public Optional<PlaceRecommendation> evaluate(
		CandidatePlace place,
		LocalDate date,
		ZoneId zone,
		int bortleClass,
		WeatherForecast forecast,
		Double originLatitude,
		Double originLongitude,
		RecommendationPolicy policy
	) {
		Optional<NightAssessment> night = nightAssessmentEngine.assess(
			place.latitude(), place.longitude(), date, zone, bortleClass, forecast.hours(), policy);

		return night.map(assessment -> {
			TravelEstimate travel = (originLatitude != null && originLongitude != null)
				? TravelEstimator.estimate(originLatitude, originLongitude, place.latitude(), place.longitude())
				: null;
			double travelHours = (travel != null) ? travel.minutes() / 60.0 : 0;
			double rank = assessment.quality() - travelHours * TRAVEL_PENALTY_PER_HOUR;

			return new PlaceRecommendation(
				place, assessment, bortleClass, travel, rank, forecast.source(), forecast.issuedAt());
		});
	}

	/**
	 * 등급이 높은 순, 등급이 같으면 rank가 큰 순으로 늘어놓는다. 그래도 같으면 이름순이다.
	 */
	public List<PlaceRecommendation> rank(List<PlaceRecommendation> recommendations) {
		Comparator<PlaceRecommendation> byLevel = Comparator.comparing(r -> r.night().level());
		Comparator<PlaceRecommendation> byRank = Comparator.comparingDouble(PlaceRecommendation::rank);

		return recommendations.stream()
			.sorted(byLevel.reversed()
				.thenComparing(byRank.reversed())
				.thenComparing(r -> r.place().name()))
			.toList();
	}
}
