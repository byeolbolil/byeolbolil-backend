package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Moon;
import xyz.ncookie.stargazer.domain.recommend.model.MoonNight;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;

@Schema(description = "한 좌표에서 본 앞으로 몇 주의 달. 예보는 보지 않는다")
public record MoonCalendarResponse(

	@Schema(description = "요청한 위도")
	double latitude,

	@Schema(description = "요청한 경도")
	double longitude,

	@Schema(description = "첫 밤(오늘 밤)의 날짜. 새벽 4시 전에는 전날이다", example = "2026-11-27")
	String startDate,

	@Schema(description = "오늘 밤부터 날짜순")
	List<Night> nights
) {

	public static MoonCalendarResponse of(double latitude, double longitude, List<MoonNight> nights, ZoneId zone) {
		return new MoonCalendarResponse(
			latitude,
			longitude,
			nights.isEmpty() ? null : nights.get(0).date().toString(),
			nights.stream().map(night -> Night.of(night, zone)).toList()
		);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "하룻밤의 달")
	public record Night(
		@Schema(description = "밤이 시작하는 날짜", example = "2026-12-09")
		String date,
		@Schema(description = "그 밤의 달. 밤 추천의 moon과 같다")
		Moon moon,
		@Schema(description = "하늘이 어두운 구간(태양 고도 -12° 이하). 밤 추천의 dark 칸과 같은 기준이다. 그만큼 어두워지지 않으면 빠진다")
		Span dark,
		@Schema(description = "어두운 구간 가운데 달이 지평선 아래에 있는 구간. 시간순이고, 달이 밤새 떠 있으면 빈 배열이다. "
			+ "달이 초저녁에 지고 새벽에 다시 뜨면 하나, 밤중에 잠깐 떠 있으면 둘이다")
		List<Span> moonless,
		@Schema(description = "moonless 구간의 길이를 더한 분. 응답에 적힌 시각으로 셈한 값이다", example = "640")
		long moonlessMinutes
	) {
		static Night of(MoonNight night, ZoneId zone) {
			return new Night(
				night.date().toString(),
				Moon.of(night.moon(), zone),
				(night.dark() != null) ? Span.of(night.dark(), zone) : null,
				night.moonless().stream().map(span -> Span.of(span, zone)).toList(),
				night.moonless().stream().mapToLong(Night::minutesShown).sum()
			);
		}

		/** 분 단위로 자른 두 시각 사이의 분. 화면에 적힌 시각으로 셈한 것과 맞추려고 자른 뒤에 뺀다 */
		private static long minutesShown(TimeSpan span) {
			return Duration.between(
				span.start().truncatedTo(ChronoUnit.MINUTES), span.end().truncatedTo(ChronoUnit.MINUTES)).toMinutes();
		}
	}

	@Schema(description = "시간 구간. 분 단위로 자른 시각이다")
	public record Span(
		@Schema(example = "2026-12-09T18:51:00+09:00")
		String start,
		@Schema(example = "2026-12-10T05:31:00+09:00")
		String end
	) {
		static Span of(TimeSpan span, ZoneId zone) {
			return new Span(
				NightRecommendationResponse.format(span.start().truncatedTo(ChronoUnit.MINUTES), zone),
				NightRecommendationResponse.format(span.end().truncatedTo(ChronoUnit.MINUTES), zone)
			);
		}
	}
}
