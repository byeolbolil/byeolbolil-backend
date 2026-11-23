package xyz.ncookie.stargazer.domain.weather.client.metnorway;

import java.time.Instant;

import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * MET Norway에서 받은 예보와, 그 응답을 다시 요청하지 말아야 하는 기한.
 *
 * @param forecast  시간별 예보
 * @param expiresAt 응답의 Expires 헤더. 헤더가 없거나 읽지 못했으면 null
 */
public record MetNorwayForecast(WeatherForecast forecast, Instant expiresAt) {}
