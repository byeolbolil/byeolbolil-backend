package xyz.ncookie.stargazer.domain.recommend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.engine.NightAssessmentEngine;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.NightMoon;
import xyz.ncookie.stargazer.domain.recommend.model.NightPattern;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceType;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

class NightRecommendationDomainServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDate DATE = LocalDate.of(2026, 11, 9);
	private static final WeatherForecast FORECAST = new WeatherForecast(
		ForecastSource.OPEN_METEO, Instant.parse("2026-11-09T00:00:00Z"), 37.5, 127.0, List.of());

	private final NightAssessmentEngine engine = mock(NightAssessmentEngine.class);
	private final NightRecommendationDomainService service = new NightRecommendationDomainService(engine);

	@Test
	@DisplayName("출발지를 모르면 이동 추정이 없고, 순위 값은 추천 구간의 질 그대로다")
	void withoutOrigin() {
		engineReturns(night(RecommendationLevel.STRONG, 80));

		PlaceRecommendation result = evaluate(place("가", 38.0, 127.0), null, null).orElseThrow();

		assertThat(result.travel()).isNull();
		assertThat(result.rank()).isEqualTo(80.0);
		assertThat(result.forecastSource()).isEqualTo(ForecastSource.OPEN_METEO);
		assertThat(result.bortleClass()).isEqualTo(3);
	}

	@Test
	@DisplayName("출발지를 알면 이동 1시간마다 순위 값에서 4를 뺀다")
	void travelLowersRank() {
		engineReturns(night(RecommendationLevel.STRONG, 80));

		// 위도 1도 거리. 이동 추정 145분이므로 145 / 60 × 4 = 9.67을 뺀다.
		PlaceRecommendation result = evaluate(place("가", 38.0, 127.0), 37.0, 127.0).orElseThrow();

		assertThat(result.travel().minutes()).isEqualTo(145);
		assertThat(result.rank()).isCloseTo(70.33, org.assertj.core.api.Assertions.within(0.01));
	}

	@Test
	@DisplayName("그 밤에 평가할 예보가 없으면 결과도 없다")
	void noAssessment() {
		when(engine.assess(anyDouble(), anyDouble(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.empty());

		assertThat(evaluate(place("가", 38.0, 127.0), null, null)).isEmpty();
	}

	@Test
	@DisplayName("등급이 높은 곳이 먼저고, 등급이 같으면 순위 값이 큰 곳이 먼저다")
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
	@DisplayName("등급과 순위 값이 모두 같으면 이름순이다")
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
		when(engine.assess(anyDouble(), anyDouble(), any(), any(), anyInt(), any(), any()))
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
			37.5, 127.0, DATE, 7, SEOUL, 3, FORECAST, RecommendationPolicy.DEFAULT);

		assertThat(nights).extracting(NightAssessment::date)
			.containsExactly(DATE, DATE.plusDays(1), DATE.plusDays(2));
	}

	private void engineReturns(NightAssessment night) {
		when(engine.assess(anyDouble(), anyDouble(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.of(night));
	}

	private Optional<PlaceRecommendation> evaluate(CandidatePlace place, Double originLat, Double originLon) {
		return service.evaluate(place, DATE, SEOUL, 3, FORECAST, originLat, originLon, RecommendationPolicy.DEFAULT);
	}

	private static PlaceRecommendation recommendation(String name, RecommendationLevel level, double rank) {
		return new PlaceRecommendation(
			place(name, 37.5, 127.0), night(level, rank), 3, null, rank, ForecastSource.OPEN_METEO, null);
	}

	private static CandidatePlace place(String name, double lat, double lon) {
		return new CandidatePlace(PlaceType.SPOT, 1L, 1L, name, "주소", lat, lon);
	}

	private static NightAssessment night(RecommendationLevel level, double quality) {
		return new NightAssessment(
			DATE, List.of(), List.of(), null, level, NightPattern.BRIEF_CLEAR, List.of(), quality,
			new NightMoon(0.5, true, null, null));
	}
}
