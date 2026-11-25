package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Instant;

import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;

/**
 * 한 장소의 하룻밤 추천 결과.
 *
 * @param light            장소의 불빛 조건
 * @param travel           출발지에서의 이동 추정. 보여 주기만 하고 순서에는 쓰지 않는다. 출발지를 모르면 null
 * @param forecastSource   예보를 받아 온 곳
 * @param forecastIssuedAt 예보가 만들어진 시각. 모르면 null
 */
public record PlaceRecommendation(
	CandidatePlace place,
	NightAssessment night,
	LightCondition light,
	TravelEstimate travel,
	ForecastSource forecastSource,
	Instant forecastIssuedAt
) {}
