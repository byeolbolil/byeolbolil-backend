package xyz.ncookie.stargazer.domain.recommend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.engine.NightAssessmentEngine;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.LightCondition;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.NightMoon;
import xyz.ncookie.stargazer.domain.recommend.model.NightPattern;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceType;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.recommend.model.TravelEstimate;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

class NightRecommendationDomainServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDate DATE = LocalDate.of(2026, 11, 9);
	private static final WeatherForecast FORECAST = new WeatherForecast(
		ForecastSource.OPEN_METEO, Instant.parse("2026-11-09T00:00:00Z"), 37.5, 127.0, List.of());

	private static final LightCondition LIGHT = new LightCondition(3.2, 0.9, 0.18);

	private final NightAssessmentEngine engine = mock(NightAssessmentEngine.class);
	private final NightRecommendationDomainService service = new NightRecommendationDomainService(engine);

	@Test
	@DisplayName("출발지를 모르면 이동 추정이 없다")
	void withoutOrigin() {
		engineReturns(night(RecommendationLevel.STRONG, 80));

		PlaceRecommendation result = evaluate(place("가", 38.0, 127.0), null, null).orElseThrow();

		assertThat(result.travel()).isNull();
		assertThat(result.night().quality()).isEqualTo(80.0);
		assertThat(result.forecastSource()).isEqualTo(ForecastSource.OPEN_METEO);
		assertThat(result.light()).isEqualTo(LIGHT);
	}

	@Test
	@DisplayName("출발지를 알면 이동 추정을 싣는다. 추천 구간의 질은 그대로다")
	void travelIsShownOnly() {
		engineReturns(night(RecommendationLevel.STRONG, 80));

		// 위도 1도 거리. 이동 추정 145분
		PlaceRecommendation result = evaluate(place("가", 38.0, 127.0), 37.0, 127.0).orElseThrow();

		assertThat(result.travel().minutes()).isEqualTo(145);
		assertThat(result.night().quality()).isEqualTo(80.0);
	}

	@Test
	@DisplayName("먼 곳이어도 더 잘 보이면 앞에 온다. 이동 시간은 순서를 바꾸지 않는다")
	void travelDoesNotChangeOrder() {
		PlaceRecommendation near = new PlaceRecommendation(
			place("가까운 곳", 37.1, 127.0), night(RecommendationLevel.STRONG, 60), LIGHT,
			new TravelEstimate(10, 13), ForecastSource.OPEN_METEO, null);
		PlaceRecommendation far = new PlaceRecommendation(
			place("먼 곳", 35.0, 129.0), night(RecommendationLevel.STRONG, 61), LIGHT,
			new TravelEstimate(400, 520), ForecastSource.OPEN_METEO, null);

		assertThat(service.rank(List.of(near, far))).extracting(r -> r.place().name())
			.containsExactly("먼 곳", "가까운 곳");
	}

	@Test
	@DisplayName("등급과 질이 모두 같으면 가까운 곳이 앞이다. 흐린 밤의 목록이 가까운 순서가 된다")
	void equalQualityNearestFirst() {
		PlaceRecommendation far = new PlaceRecommendation(
			place("가 먼 곳", 35.0, 129.0), night(RecommendationLevel.NO, 0), LIGHT,
			new TravelEstimate(400, 520), ForecastSource.OPEN_METEO, null);
		PlaceRecommendation near = new PlaceRecommendation(
			place("나 가까운 곳", 37.1, 127.0), night(RecommendationLevel.NO, 0), LIGHT,
			new TravelEstimate(10, 13), ForecastSource.OPEN_METEO, null);

		assertThat(service.rank(List.of(far, near))).extracting(r -> r.place().name())
			.containsExactly("나 가까운 곳", "가 먼 곳");
	}

	@Test
	@DisplayName("이동 시간 한도에는 6분의 1만큼 여유가 있다. 2시간 한도면 2시간 20분까지 든다")
	void travelLimitHasSlack() {
		assertThat(service.isWithinReach(new TravelEstimate(100, 120), 120)).isTrue();
		assertThat(service.isWithinReach(new TravelEstimate(110, 140), 120)).isTrue();
		assertThat(service.isWithinReach(new TravelEstimate(111, 141), 120)).isFalse();
		assertThat(service.isWithinReach(new TravelEstimate(160, 210), 180)).isTrue();
		assertThat(service.isWithinReach(new TravelEstimate(161, 211), 180)).isFalse();
		assertThat(service.isWithinReach(new TravelEstimate(50, 70), 60)).isTrue();
		assertThat(service.isWithinReach(new TravelEstimate(51, 71), 60)).isFalse();
	}

	@Test
	@DisplayName("한도가 없거나 이동 추정이 없으면 모두 한도 안이고, 한도가 0이면 제자리만 든다")
	void travelLimitEdges() {
		assertThat(service.isWithinReach(new TravelEstimate(400, 520), null)).isTrue();
		assertThat(service.isWithinReach(null, 60)).isTrue();
		assertThat(service.isWithinReach(new TravelEstimate(0, 0), 0)).isTrue();
		assertThat(service.isWithinReach(new TravelEstimate(1, 1), 0)).isFalse();
	}

	@Test
	@DisplayName("그 밤에 평가할 예보가 없으면 결과도 없다")
	void noAssessment() {
		when(engine.assess(anyDouble(), anyDouble(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());

		assertThat(evaluate(place("가", 38.0, 127.0), null, null)).isEmpty();
	}

	@Test
	@DisplayName("등급이 높은 곳이 먼저고, 등급이 같으면 추천 구간의 질이 높은 곳이 먼저다")
	void ranksByLevelThenRank() {
		List<PlaceRecommendation> ranked = service.rank(List.of(
			recommendation("가까운 가볼 만한 곳", RecommendationLevel.GOOD, 95),
			recommendation("먼 꼭 가볼 만한 곳", RecommendationLevel.STRONG, 10),
			recommendation("쉬어가는 곳", RecommendationLevel.NO, 0),
			recommendation("좋은 꼭 가볼 만한 곳", RecommendationLevel.STRONG, 60)
		));

		assertThat(ranked).extracting(r -> r.place().name()).containsExactly(
			"좋은 꼭 가볼 만한 곳",
			"먼 꼭 가볼 만한 곳",
			"가까운 가볼 만한 곳",
			"쉬어가는 곳"
		);
	}

	@Test
	@DisplayName("등급과 추천 구간의 질이 같고 이동 시간도 모르면 이름순이다")
	void tieBrokenByName() {
		List<PlaceRecommendation> ranked = service.rank(List.of(
			recommendation("나", RecommendationLevel.NO, 0),
			recommendation("가", RecommendationLevel.NO, 0)
		));

		assertThat(ranked).extracting(r -> r.place().name()).containsExactly("가", "나");
	}

	@Test
	@DisplayName("여러 밤을 날짜순으로 평가하고, 예보가 닿지 않는 밤은 뺀다")
	void assessNightsSkipsNightsWithoutForecast() {
		when(engine.assess(anyDouble(), anyDouble(), any(), any(), any(), any(), any()))
			.thenAnswer(call -> {
				LocalDate date = call.getArgument(2);
				// 넷째 밤부터는 예보가 없다
				if (!date.isBefore(DATE.plusDays(3))) {
					return Optional.empty();
				}
				return Optional.of(new NightAssessment(
					date, List.of(), List.of(), null, RecommendationLevel.GOOD, NightPattern.BRIEF_CLEAR, List.of(), 50,
					new NightMoon(0.5, true, null, null)));
			});

		List<NightAssessment> nights = service.assessNights(
			37.5, 127.0, DATE, 7, SEOUL, LIGHT, FORECAST, RecommendationPolicy.DEFAULT);

		assertThat(nights).extracting(NightAssessment::date)
			.containsExactly(DATE, DATE.plusDays(1), DATE.plusDays(2));
	}

	@Test
	@DisplayName("밤마다 장소들 가운데 가장 높은 등급을 고르고, 아무도 평가하지 못한 밤은 넣지 않는다")
	void bestLevelByNight() {
		List<NightAssessment> first = List.of(
			night(DATE, RecommendationLevel.NO),
			night(DATE.plusDays(1), RecommendationLevel.STRONG),
			night(DATE.plusDays(2), RecommendationLevel.MAYBE));
		// 예보가 짧아 첫 밤만 평가한 장소
		List<NightAssessment> second = List.of(night(DATE, RecommendationLevel.GOOD));
		List<NightAssessment> third = List.of(
			night(DATE, RecommendationLevel.MAYBE),
			night(DATE.plusDays(1), RecommendationLevel.GOOD),
			night(DATE.plusDays(2), RecommendationLevel.NO));

		assertThat(service.bestLevelByNight(List.of(first, second, third))).containsExactly(
			Map.entry(DATE, RecommendationLevel.GOOD),
			Map.entry(DATE.plusDays(1), RecommendationLevel.STRONG),
			Map.entry(DATE.plusDays(2), RecommendationLevel.MAYBE));
	}

	@Test
	@DisplayName("평가한 장소가 없으면 밤마다의 등급도 없다")
	void bestLevelByNightWithoutPlaces() {
		assertThat(service.bestLevelByNight(List.of())).isEmpty();
		assertThat(service.bestLevelByNight(List.of(List.of(), List.of()))).isEmpty();
	}

	@Test
	@DisplayName("밤마다 GOOD 이상인 장소를 센다. MAYBE는 세지 않고, 아무도 좋지 않은 밤은 0이다")
	void goodCountByNight() {
		List<NightAssessment> first = List.of(
			night(DATE, RecommendationLevel.STRONG),
			night(DATE.plusDays(1), RecommendationLevel.MAYBE),
			night(DATE.plusDays(2), RecommendationLevel.NO));
		List<NightAssessment> second = List.of(night(DATE, RecommendationLevel.GOOD));
		List<NightAssessment> third = List.of(
			night(DATE, RecommendationLevel.MAYBE),
			night(DATE.plusDays(1), RecommendationLevel.GOOD),
			night(DATE.plusDays(2), RecommendationLevel.MAYBE));

		assertThat(service.goodCountByNight(List.of(first, second, third))).containsExactly(
			Map.entry(DATE, 2),
			Map.entry(DATE.plusDays(1), 1),
			Map.entry(DATE.plusDays(2), 0));
		assertThat(service.goodCountByNight(List.of())).isEmpty();
	}

	@Test
	@DisplayName("이동 추정은 출발지의 위도와 경도를 모두 알 때만 있다")
	void travelToNeedsOrigin() {
		CandidatePlace place = place("가", 38.0, 127.0);

		assertThat(service.travelTo(place, null, null)).isNull();
		assertThat(service.travelTo(place, 37.0, null)).isNull();
		assertThat(service.travelTo(place, 37.0, 127.0).minutes()).isEqualTo(145);
	}

	private void engineReturns(NightAssessment night) {
		when(engine.assess(anyDouble(), anyDouble(), any(), any(), any(), any(), any())).thenReturn(Optional.of(night));
	}

	private Optional<PlaceRecommendation> evaluate(CandidatePlace place, Double originLat, Double originLon) {
		return service.evaluate(place, DATE, SEOUL, LIGHT, FORECAST, originLat, originLon, RecommendationPolicy.DEFAULT);
	}

	private static PlaceRecommendation recommendation(String name, RecommendationLevel level, double rank) {
		return new PlaceRecommendation(
			place(name, 37.5, 127.0), night(level, rank), LIGHT, null, ForecastSource.OPEN_METEO, null);
	}

	private static CandidatePlace place(String name, double lat, double lon) {
		return new CandidatePlace(PlaceType.SPOT, 1L, 1L, name, "주소", lat, lon);
	}

	private static NightAssessment night(LocalDate date, RecommendationLevel level) {
		return new NightAssessment(
			date, List.of(), List.of(), null, level, NightPattern.BRIEF_CLEAR, List.of(), 50,
			new NightMoon(0.5, true, null, null));
	}

	private static NightAssessment night(RecommendationLevel level, double quality) {
		return new NightAssessment(
			DATE, List.of(), List.of(), null, level, NightPattern.BRIEF_CLEAR, List.of(), quality,
			new NightMoon(0.5, true, null, null));
	}
}
