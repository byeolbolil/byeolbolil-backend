package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import xyz.ncookie.stargazer.domain.weather.model.ForecastSource;

/**
 * 한 장소의 이번 주 밤.
 *
 * @param place            좌표로만 고른 위치면 종류와 이름이 없다
 * @param nights           날짜순. 예보가 닿지 않는 밤은 빠져 있다. 예보를 받지 못한 장소는 빈 목록이다
 * @param forecastSource   예보를 받지 못했으면 null
 * @param forecastIssuedAt 모르면 null
 * @param skies            날짜순의 그 밤 하늘(은하수와 현상). 예보와 상관없이 볼 수 있는 밤 전부다
 */
public record WeekOutlook(
	CandidatePlace place,
	LightCondition light,
	List<NightAssessment> nights,
	ForecastSource forecastSource,
	Instant forecastIssuedAt,
	List<NightSky> skies
) {

	public WeekOutlook {
		nights = List.copyOf(nights);
		skies = List.copyOf(skies);
	}

	/** 그 날짜의 밤 하늘. 없으면 null */
	public NightSky skyOn(LocalDate date) {
		return skies.stream().filter(sky -> sky.date().equals(date)).findFirst().orElse(null);
	}
}
