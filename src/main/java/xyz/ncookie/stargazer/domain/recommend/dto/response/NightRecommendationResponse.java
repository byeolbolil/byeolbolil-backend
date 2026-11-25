package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.model.ClearWindow;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.NightHour;
import xyz.ncookie.stargazer.domain.recommend.model.NightMoon;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceGuide;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "하룻밤의 장소별 추천. places는 추천 순서대로이고 첫 번째가 가장 좋은 곳이다")
public record NightRecommendationResponse(

	@Schema(description = "밤이 시작하는 날짜", example = "2026-11-24")
	String date,

	@Schema(description = "추천에 쓴 기준")
	Criteria criteria,

	@Schema(description = "후보 장소 수")
	int candidateCount,

	@Schema(description = "예보를 받아 평가한 장소 수. candidateCount보다 작으면 일부 장소의 예보를 받지 못한 것이다")
	int evaluatedCount,

	@Schema(description = "이동 시간 한도를 넘어 뺀 장소 수")
	int excludedByTravelCount,

	List<Place> places
) {

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	@Schema(description = "추천에 쓴 기준")
	public record Criteria(
		@Schema(description = "'별 보기 아주 좋아요'가 되는 맑은 시간", example = "3")
		int minClearHours,
		@Schema(description = "이 시각 전에 맑은 시간을 우선", example = "02:00")
		String preferBefore,
		@Schema(description = "이동 시간 한도(분). 없으면 빠진다")
		Integer maxTravelMinutes
	) {
		public static Criteria of(RecommendationPolicy policy, Integer maxTravelMinutes) {
			return new Criteria(
				policy.minClearHours(),
				policy.preferBefore().format(DateTimeFormatter.ofPattern("HH:mm")),
				maxTravelMinutes
			);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "장소 하나의 추천")
	public record Place(
		@Schema(description = "SPOT은 기본 추천 명소, BOOKMARK는 내 장소")
		String type,
		@Schema(description = "type이 SPOT이면 명소 ID, BOOKMARK면 북마크 ID")
		Long id,
		@Schema(description = "명소이거나 명소를 저장한 북마크일 때 그 명소의 ID")
		Long spotId,
		String name,
		String address,
		double latitude,
		double longitude,

		@Schema(description = "실제로 차를 대고 보는 지점. 좌표가 가리키는 곳이다. 명소에만 있다", example = "멍에전망대 주차장")
		String viewpoint,
		@Schema(description = "어떤 곳인지 한두 문장. 명소에만 있다")
		String description,
		@Schema(description = "가는 법과 주차. 명소에만 있다")
		String guide,
		@Schema(description = "계절 폐쇄, 조명, 통제 같은 주의 사항. 없으면 빠진다")
		String caution,
		@Schema(description = "가는 길과 서는 자리를 더 자세히 적은 글. 없으면 빠진다")
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		List<Link> links,

		@Schema(description = "추천 등급. STRONG 별 보기 아주 좋아요, GOOD 별 보기 좋아요, MAYBE 별 보기 아쉬워요, NO 별 보기 어려워요")
		String level,
		@Schema(description = "밤 패턴. CLEAR_ALL_NIGHT, CLEAR_EARLY, CLEARS_LATE, CLEARS_AGAIN, BRIEF_CLEAR, CLOUDY_ALL_NIGHT")
		String pattern,
		@Schema(description = "주의 사항. CLEARS_LATE, BRIGHT_MOON, CITY_LIGHTS, LIGHTS_IN_VIEW, DEW")
		List<String> notes,
		@Schema(description = "추천 시간대. 맑은 시간이 없으면 빠진다")
		Window bestWindow,

		@Schema(description = "이동 추정. 출발지를 보내지 않았으면 빠진다")
		Travel travel,
		@Schema(description = "하늘 전체의 인공 밝기가 자연 밤하늘의 몇 배인지(ALR). 자료가 없는 곳이면 빠진다", example = "3.99")
		Double skyBrightness,
		@Schema(description = "하늘 밝기로 나눈 등급. NATURAL, DARK, RURAL, SUBURBAN, URBAN. 자료가 없는 곳이면 빠진다")
		String skyClass,
		@Schema(description = "없어질 값. skyBrightness에서 어림한 Bortle 등급이다", deprecated = true)
		int bortleClass,
		@Schema(description = "보름달을 1로 본 그 밤의 달 밝기", example = "0.09")
		double moonBrightness,
		@Schema(description = "그 밤의 달 모양과 뜨고 지는 시각")
		Moon moon,

		@Schema(description = "예보 출처. OPEN_METEO 또는 MET_NORWAY")
		String forecastSource,
		@Schema(description = "예보가 만들어진 시각. 모르면 빠진다")
		String forecastIssuedAt,

		@Schema(description = "해가 진 뒤부터 뜨기 전까지의 시간별 조건")
		List<Hour> hours
	) {
		public static Place of(PlaceRecommendation recommendation, ZoneId zone) {
			NightAssessment night = recommendation.night();
			PlaceGuide guide = recommendation.place().guide();
			return new Place(
				recommendation.place().type().name(),
				recommendation.place().id(),
				recommendation.place().spotId(),
				recommendation.place().name(),
				recommendation.place().address(),
				recommendation.place().latitude(),
				recommendation.place().longitude(),
				guide != null ? guide.viewpoint() : null,
				guide != null ? guide.description() : null,
				guide != null ? guide.guide() : null,
				guide != null ? guide.caution() : null,
				guide != null ? guide.links().stream().map(link -> new Link(link.title(), link.url(), link.imageUrl())).toList() : null,
				night.level().name(),
				night.pattern().name(),
				night.notes().stream().map(Enum::name).toList(),
				night.bestWindow() != null ? Window.of(night.bestWindow(), zone) : null,
				recommendation.travel() != null
					? new Travel(round1(recommendation.travel().distanceKm()), recommendation.travel().minutes())
					: null,
				SkyValues.brightness(recommendation.light()),
				SkyValues.className(recommendation.light()),
				recommendation.light().legacyBortleClass(),
				moonBrightness(night),
				Moon.of(night.moon(), zone),
				recommendation.forecastSource().name(),
				recommendation.forecastIssuedAt() != null ? format(recommendation.forecastIssuedAt(), zone) : null,
				night.hours().stream().map(hour -> Hour.of(hour, zone)).toList()
			);
		}

		/** 달의 위상은 하룻밤 사이에 거의 변하지 않으므로 첫 어두운 칸의 값을 그 밤의 값으로 쓴다. */
		static double moonBrightness(NightAssessment night) {
			return night.hours().stream()
				.filter(NightHour::dark)
				.findFirst()
				.map(hour -> Math.round(hour.moonBrightness() * 100) / 100.0)
				.orElse(0.0);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "참고 링크")
	public record Link(
		@Schema(description = "그 글의 제목")
		String title,
		String url,
		@Schema(description = "그 글의 대표 이미지. 링크의 미리보기로 쓴다. 없으면 빠진다")
		String imageUrl
	) {}

	@Schema(description = "그 장소에서 본 그 밤의 달")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Moon(
		@Schema(description = "밝게 보이는 면의 비율. 삭 0, 보름 1", example = "0.52")
		double illuminatedFraction,
		@Schema(description = "차오르는 중이면 true, 기우는 중이면 false")
		boolean waxing,
		@Schema(description = "그날 정오부터 다음 날 정오 사이에 달이 뜨는 시각. 그 사이에 뜨지 않으면 빠진다",
			example = "2026-11-24T17:02:00+09:00")
		String rise,
		@Schema(description = "같은 사이에 달이 지는 시각. 그 사이에 지지 않으면 빠진다",
			example = "2026-11-25T07:41:00+09:00")
		String set
	) {
		static Moon of(NightMoon moon, ZoneId zone) {
			return new Moon(
				Math.round(moon.illuminatedFraction() * 100) / 100.0,
				moon.waxing(),
				moon.rise() != null ? format(moon.rise().truncatedTo(ChronoUnit.MINUTES), zone) : null,
				moon.set() != null ? format(moon.set().truncatedTo(ChronoUnit.MINUTES), zone) : null
			);
		}
	}

	@Schema(description = "시간대")
	public record Window(
		@Schema(example = "2026-11-24T19:00:00+09:00")
		String start,
		@Schema(example = "2026-11-25T02:00:00+09:00")
		String end,
		@Schema(description = "길이(시간)", example = "7")
		int hours
	) {
		static Window of(ClearWindow window, ZoneId zone) {
			return new Window(format(window.start(), zone), format(window.end(), zone), window.hours());
		}
	}

	@Schema(description = "이동 추정. 직선거리의 1.3배를 시속 60km로 간다고 본 값이다")
	public record Travel(
		@Schema(description = "직선거리(km)", example = "128.4")
		double distanceKm,
		@Schema(description = "차로 걸리는 시간 추정(분)", example = "167")
		int minutes
	) {}

	@Schema(description = "한 시간 칸의 조건. time부터 1시간이다")
	public record Hour(
		@Schema(example = "2026-11-24T19:00:00+09:00")
		String time,
		@Schema(description = "태양이 -12° 아래에 있는 어두운 시간인지")
		boolean dark,
		@Schema(description = "어둡고 구름이 30% 이하인 맑은 시간인지")
		boolean clear,
		@Schema(description = "총운량(%)")
		int cloudCover,
		int cloudCoverLow,
		int cloudCoverMid,
		int cloudCoverHigh,
		@Schema(description = "상대습도(%)")
		int relativeHumidity,
		@Schema(description = "달이 떠 있는지")
		boolean moonUp,
		@Schema(description = "0~100. 어둡지 않은 시간은 0이다")
		int score
	) {
		static Hour of(NightHour hour, ZoneId zone) {
			return new Hour(
				format(hour.time(), zone),
				hour.dark(),
				hour.clear(),
				hour.cloudCover(),
				hour.cloudCoverLow(),
				hour.cloudCoverMid(),
				hour.cloudCoverHigh(),
				hour.relativeHumidity(),
				hour.moonUp(),
				hour.score()
			);
		}
	}

	private static String format(Instant instant, ZoneId zone) {
		return TIME_FORMAT.format(instant.atZone(zone).toOffsetDateTime());
	}

	private static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}
}
