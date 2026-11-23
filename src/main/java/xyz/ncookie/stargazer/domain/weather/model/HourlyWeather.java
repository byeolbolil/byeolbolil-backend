package xyz.ncookie.stargazer.domain.weather.model;

import java.time.Instant;

/**
 * 한 시각의 예보 값.
 *
 * @param time             예보 대상 시각(정시)
 * @param cloudCover       총운량 (%)
 * @param cloudCoverLow    하층운량 (%)
 * @param cloudCoverMid    중층운량 (%)
 * @param cloudCoverHigh   상층운량 (%)
 * @param relativeHumidity 지상 2m 상대습도 (%)
 * @param dewPoint         지상 2m 이슬점 (°C)
 * @param temperature      지상 2m 기온 (°C)
 * @param visibility       시정 (m). MET Norway는 시정을 주지 않아서 그때는 null이다
 */
public record HourlyWeather(
	Instant time,
	int cloudCover,
	int cloudCoverLow,
	int cloudCoverMid,
	int cloudCoverHigh,
	int relativeHumidity,
	double dewPoint,
	double temperature,
	Integer visibility
) {}
