package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 출발지에서 장소까지의 이동 추정.
 *
 * @param distanceKm 직선거리 (km)
 * @param minutes    차로 걸리는 시간 추정 (분)
 */
public record TravelEstimate(double distanceKm, int minutes) {}
