package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Instant;

import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;

/**
 * 한 장소의 하룻밤 추천 결과.
 *
 * @param light            장소의 불빛 조건
 * @param travel           출발지를 모르면 null
 * @param rank             같은 등급 안에서 순서를 정하는 값. 추천 구간의 질에서 이동 시간만큼을 뺀다
 * @param forecastSource   예보를 받아 온 곳
 * @param forecastIssuedAt 예보가 만들어진 시각. 모르면 null
 */
public record PlaceRecommendation(
	CandidatePlace place,
	NightAssessment night,
	LightCondition light,
	TravelEstimate travel,
	double rank,
	ForecastSource forecastSource,
	Instant forecastIssuedAt
) {}
