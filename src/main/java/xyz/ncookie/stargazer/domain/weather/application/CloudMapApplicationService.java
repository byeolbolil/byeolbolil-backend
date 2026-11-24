package xyz.ncookie.stargazer.domain.weather.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.weather.domain.WeatherDomainService;
import xyz.ncookie.stargazer.domain.weather.dto.response.CloudMapResponse;
import xyz.ncookie.stargazer.domain.weather.model.CloudGridForecast;

@Service
@RequiredArgsConstructor
public class CloudMapApplicationService {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	// 하룻밤으로 잘라 주는 범위. 밤 추천의 시간 칸은 해가 지평선 아래 12°보다 낮은 시간이라 한겨울에도
	// 저녁 5시부터 다음 날 아침 8시 사이에 든다. 넉넉히 잡은 운영상의 값이고 판정에는 쓰지 않는다.
	private static final LocalTime NIGHT_FROM = LocalTime.of(17, 0);
	private static final LocalTime NIGHT_UNTIL = LocalTime.of(8, 0);

	private final WeatherDomainService weatherDomainService;

	/**
	 * date에 시작하는 밤의 구름 격자. 예보가 닿지 않는 날짜면 시간 칸이 비어 있다.
	 */
	public CloudMapResponse getNight(LocalDate date) {
		Instant from = date.atTime(NIGHT_FROM).atZone(ZONE).toInstant();
		Instant until = date.plusDays(1).atTime(NIGHT_UNTIL).atZone(ZONE).toInstant();

		CloudGridForecast night = weatherDomainService.getCloudGrid().between(from, until);
		return CloudMapResponse.of(date, night, ZONE);
	}
}
