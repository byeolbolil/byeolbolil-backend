package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Moon;
import xyz.ncookie.stargazer.domain.recommend.model.MoonNight;
import xyz.ncookie.stargazer.domain.recommend.model.NightSky;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;

@Schema(description = "한 좌표에서 본 앞으로 몇 주의 달. 예보는 보지 않는다")
public record MoonCalendarResponse(

	@Schema(description = "요청한 위도")
	double latitude,

	@Schema(description = "요청한 경도")
	double longitude,

	@Schema(description = "첫 밤(오늘 밤)의 날짜. 새벽 4시 전에는 전날이다", example = "2026-11-27")
	String startDate,

	@Schema(description = "오늘 밤부터 처음으로 은하수 중심이 보이는 밤. 겨울에 언제부터 다시 보이는지 알려 주는 데 쓴다. "
		+ "오늘 밤에 보이면 startDate와 같고, 한 해 안에 보이지 않는 자리면 null이다", example = "2027-02-08")
	String milkyWayNextDate,

	@Schema(description = "오늘 밤부터 날짜순")
	List<Night> nights
) {

	/**
	 * @param skies            nights와 같은 날짜순의 그 밤 하늘
	 * @param milkyWayNextDate 처음으로 은하수 중심이 보이는 밤. 없으면 null
	 */
	public static MoonCalendarResponse of(
		double latitude, double longitude, List<MoonNight> nights, List<NightSky> skies, LocalDate milkyWayNextDate,
		ZoneId zone
	) {
		Map<LocalDate, NightSky> skyByDate = skies.stream().collect(Collectors.toMap(NightSky::date, sky -> sky));
		return new MoonCalendarResponse(
			latitude,
			longitude,
			nights.isEmpty() ? null : nights.get(0).date().toString(),
			milkyWayNextDate != null ? milkyWayNextDate.toString() : null,
			nights.stream().map(night -> Night.of(night, skyByDate.get(night.date()), zone)).toList()
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
		long moonlessMinutes,
		@Schema(description = "은하수 중심이 보이는 시간. 밤 추천의 milkyWay와 같다. 그 밤에 뜨지 않으면 빠진다")
		NightSkyViews.MilkyWay milkyWay,
		@Schema(description = "그 밤을 보라고 권하는 천문 현상과 이 자리에서 보이는 구간. 없으면 빠진다")
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		List<NightSkyViews.Event> events
	) {
		static Night of(MoonNight night, NightSky sky, ZoneId zone) {
			return new Night(
				night.date().toString(),
				Moon.of(night.moon(), zone),
				(night.dark() != null) ? Span.of(night.dark(), zone) : null,
				night.moonless().stream().map(span -> Span.of(span, zone)).toList(),
				night.moonless().stream().mapToLong(Night::minutesShown).sum(),
				NightSkyViews.milkyWayOf(sky, zone),
				NightSkyViews.eventsOf(sky, zone)
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
