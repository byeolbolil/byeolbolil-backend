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

	// 정책: 이동 시간 한도에 한도의 6분의 1만큼 여유를 둔다. 2시간 한도면 2시간 20분까지 든다.
	// 이동 시간은 직선거리로 어림한 값이라(TravelEstimator) 10~20분 차이로 장소를 빼는 것은 값의 정밀도에 맞지 않는다.
	// 근거 자료는 없고, "2시간 10분, 20분이면 2시간 거리로 봐도 된다"는 써 본 사람의 말에 맞춘 크기다.
	static final int TRAVEL_LIMIT_SLACK_DIVISOR = 6;

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

			return new PlaceRecommendation(
				place, assessment, light, travel, forecast.source(), forecast.issuedAt());
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
	 * 이동 시간 한도 안인지. 한도가 없거나 출발지를 몰라 이동 추정이 없으면 모두 한도 안으로 본다.
	 * 한도를 조금 넘는 곳은 여유(TRAVEL_LIMIT_SLACK_DIVISOR)만큼 받아 준다.
	 */
	public boolean isWithinReach(TravelEstimate travel, Integer maxTravelMinutes) {
		if (maxTravelMinutes == null || travel == null) {
			return true;
		}
		return travel.minutes() <= maxTravelMinutes + maxTravelMinutes / TRAVEL_LIMIT_SLACK_DIVISOR;
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
	 * 등급이 높은 순, 등급이 같으면 추천 구간의 질이 높은 순으로 늘어놓는다.
	 *
	 * 이동 시간은 질과 맞바꾸지 않는다. 맨 앞의 장소는 그 밤에 별이 가장 잘 보이는 곳이어야 하고, 얼마나 먼지는
	 * 보는 사람이 이동 시간 표시와 한도 설정으로 정한다(docs/decisions.md 2026-11-26 "순위에서 이동 시간을 뺀다").
	 * 질까지 같을 때만 가까운 곳을 앞에 둔다. 맑은 시간이 없는 곳은 질이 모두 0이라, 전국이 흐린 밤에는 가까운 순서가 된다.
	 * 그래도 같으면 이름순이다.
	 */
	public List<PlaceRecommendation> rank(List<PlaceRecommendation> recommendations) {
		Comparator<PlaceRecommendation> byLevel = Comparator.comparing(r -> r.night().level());
		Comparator<PlaceRecommendation> byQuality = Comparator.comparingDouble(r -> r.night().quality());
		Comparator<PlaceRecommendation> byTravel = Comparator.comparingInt(
			r -> (r.travel() != null) ? r.travel().minutes() : 0);

		return recommendations.stream()
			.sorted(byLevel.reversed()
				.thenComparing(byQuality.reversed())
				.thenComparing(byTravel)
				.thenComparing(r -> r.place().name()))
			.toList();
	}
}
