package xyz.ncookie.stargazer.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 하룻밤의 구름 값으로 Open-Meteo 예보 응답 본문을 만든다.
 *
 * 추천 시나리오 테스트에서 장소마다 다른 밤을 주는 데 쓴다. 형식은 실제 응답
 * (fixtures/openmeteo/forecast-ecmwf-ifs-anbandegi-20261123.json)과 같고, 값만 테스트가 정한다.
 */
public final class OpenMeteoNight {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private OpenMeteoNight() {
	}

	/**
	 * @param date     밤이 시작하는 날짜
	 * @param clouds   그날 저녁 6시 칸부터 한 시간씩의 총운량(%)
	 * @param humidity 모든 칸에 넣을 습도(%)
	 */
	public static String body(LocalDate date, int[] clouds, int humidity) {
		Instant start = ZonedDateTime.of(date, LocalTime.of(18, 0), SEOUL).toInstant();
		String times = IntStream.range(0, clouds.length)
			.mapToObj(i -> String.valueOf(start.getEpochSecond() + 3600L * i))
			.collect(Collectors.joining(","));
		String cloudValues = join(clouds);

		return """
			{
			  "latitude": 37.5, "longitude": 127.0,
			  "hourly": {
			    "time": [%s],
			    "cloud_cover": [%s],
			    "cloud_cover_low": [%s],
			    "cloud_cover_mid": [%s],
			    "cloud_cover_high": [%s],
			    "relative_humidity_2m": [%s],
			    "dew_point_2m": [%s],
			    "temperature_2m": [%s],
			    "visibility": [%s]
			  }
			}
			""".formatted(
			times,
			cloudValues,
			cloudValues,
			repeat("0", clouds.length),
			repeat("0", clouds.length),
			repeat(String.valueOf(humidity), clouds.length),
			repeat("-2.0", clouds.length),
			repeat("3.0", clouds.length),
			repeat("20000.0", clouds.length)
		);
	}

	/** 저녁 6시부터 아침 6시 칸까지 모두 같은 구름인 밤 */
	public static String uniform(LocalDate date, int cloud) {
		int[] clouds = new int[13];
		Arrays.fill(clouds, cloud);
		return body(date, clouds, 60);
	}

	private static String join(int[] values) {
		return Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(","));
	}

	private static String repeat(String value, int count) {
		return String.join(",", java.util.Collections.nCopies(count, value));
	}
}
