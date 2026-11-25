package xyz.ncookie.stargazer.domain.recommend.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.engine.NightAssessmentEngine;
import xyz.ncookie.stargazer.domain.recommend.engine.TravelEstimator;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.LightCondition;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
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
		LightCondition light,
		WeatherForecast forecast,
		Double originLatitude,
		Double originLongitude,
		RecommendationPolicy policy
	) {
		Optional<NightAssessment> night = nightAssessmentEngine.assess(
			place.latitude(), place.longitude(), date, zone, light, forecast.hours(), policy);

		return night.map(assessment -> {
			TravelEstimate travel = travelTo(place, originLatitude, originLongitude);
			double travelHours = (travel != null) ? travel.minutes() / 60.0 : 0;
			double rank = assessment.quality() - travelHours * TRAVEL_PENALTY_PER_HOUR;

			return new PlaceRecommendation(
				place, assessment, light, travel, rank, forecast.source(), forecast.issuedAt());
		});
	}

	/**
	 * 출발지에서 장소까지의 이동 추정. 날짜와 상관없는 값이다.
	 *
	 * @return 출발지를 모르면 null
	 */
	public TravelEstimate travelTo(CandidatePlace place, Double originLatitude, Double originLongitude) {
		if (originLatitude == null || originLongitude == null) {
			return null;
		}
		return TravelEstimator.estimate(originLatitude, originLongitude, place.latitude(), place.longitude());
	}

	/**
	 * 밤마다 장소들 가운데 가장 높은 등급을 고른다. rank()가 등급을 먼저 보므로, 같은 장소들로 만든 그 밤 추천의
	 * 첫 장소 등급과 같다.
	 *
	 * @param nightsByPlace 장소마다 평가한 밤들
	 * @return 평가한 장소가 하나도 없는 밤은 들어 있지 않다
	 */
	public Map<LocalDate, RecommendationLevel> bestLevelByNight(List<List<NightAssessment>> nightsByPlace) {
		Map<LocalDate, RecommendationLevel> best = new TreeMap<>();
		for (List<NightAssessment> nights : nightsByPlace) {
			for (NightAssessment night : nights) {
				best.merge(night.date(), night.level(), (a, b) -> b.isAbove(a) ? b : a);
			}
		}
		return best;
	}

	/**
	 * 한 장소의 여러 밤을 같은 예보로 평가한다. 이동 시간은 보지 않는다.
	 *
	 * @param startDate 첫 밤이 시작하는 날짜
	 * @param days      볼 밤의 수
	 * @return 날짜순. 예보가 닿지 않는 밤은 빠진다
	 */
	public List<NightAssessment> assessNights(
		double latitude,
		double longitude,
		LocalDate startDate,
		int days,
		ZoneId zone,
		LightCondition light,
		WeatherForecast forecast,
		RecommendationPolicy policy
	) {
		return startDate.datesUntil(startDate.plusDays(days))
			.map(date -> nightAssessmentEngine.assess(
				latitude, longitude, date, zone, light, forecast.hours(), policy))
			.flatMap(Optional::stream)
			.toList();
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
