package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Hour;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Moon;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Window;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.recommend.model.WeekOutlook;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "장소별 이번 주 밤. 오늘 밤부터 7일을 본다")
public record WeekOutlookResponse(

	@Schema(description = "오늘 밤의 날짜. 새벽 4시 전에는 전날이다", example = "2026-11-25")
	String startDate,

	@Schema(description = "볼 수 있는 밤의 날짜. startDate부터 7일이다")
	List<String> dates,

	@Schema(description = "평가에 쓴 기준")
	Criteria criteria,

	@Schema(description = "장소별 결과. 좌표로 물으면 그 위치 하나, 내 장소를 물으면 저장한 순서대로다")
	List<Place> places
) {

	public static WeekOutlookResponse of(
		LocalDate startDate, int days, RecommendationPolicy policy, List<WeekOutlook> outlooks, ZoneId zone
	) {
		return new WeekOutlookResponse(
			startDate.toString(),
			startDate.datesUntil(startDate.plusDays(days)).map(LocalDate::toString).toList(),
			new Criteria(policy.minClearHours(), policy.preferBefore().format(DateTimeFormatter.ofPattern("HH:mm"))),
			outlooks.stream().map(outlook -> Place.of(outlook, zone)).toList()
		);
	}

	@Schema(description = "평가에 쓴 기준. 이동 시간은 보지 않으므로 한도가 없다")
	public record Criteria(
		@Schema(description = "'별 보기 아주 좋아요'가 되는 맑은 시간", example = "3")
		int minClearHours,
		@Schema(description = "이 시각 전에 맑은 시간을 우선", example = "02:00")
		String preferBefore
	) {}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "장소 하나의 이번 주")
	public record Place(
		@Schema(description = "내 장소면 BOOKMARK. 좌표로 물은 위치에서는 빠진다")
		String type,
		@Schema(description = "북마크 ID. 좌표로 물은 위치에서는 빠진다")
		Long id,
		@Schema(description = "명소를 저장한 북마크일 때 그 명소의 ID")
		Long spotId,
		@Schema(description = "좌표로 물은 위치에서는 빠진다")
		String name,
		String address,
		double latitude,
		double longitude,

		@Schema(description = "광해 등급(Bortle 1~9)")
		int bortleClass,
		@Schema(description = "예보 출처. OPEN_METEO 또는 MET_NORWAY. 예보를 받지 못했으면 빠진다")
		String forecastSource,
		@Schema(description = "예보가 만들어진 시각. 모르면 빠진다")
		String forecastIssuedAt,

		@Schema(description = "날짜순의 밤. 예보가 닿지 않는 밤은 빠지고, 예보를 받지 못한 장소는 빈 배열이다")
		List<Night> nights
	) {
		static Place of(WeekOutlook outlook, ZoneId zone) {
			CandidatePlace place = outlook.place();
			return new Place(
				place.type() != null ? place.type().name() : null,
				place.id(),
				place.spotId(),
				place.name(),
				place.address(),
				place.latitude(),
				place.longitude(),
				outlook.bortleClass(),
				outlook.forecastSource() != null ? outlook.forecastSource().name() : null,
				outlook.forecastIssuedAt() != null
					? outlook.forecastIssuedAt().atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
					: null,
				outlook.nights().stream().map(night -> Night.of(night, zone)).toList()
			);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "하룻밤의 평가. 밤 추천의 장소 항목과 같은 값이다")
	public record Night(
		@Schema(description = "밤이 시작하는 날짜", example = "2026-11-25")
		String date,
		@Schema(description = "추천 등급. STRONG, GOOD, MAYBE, NO")
		String level,
		@Schema(description = "밤 패턴. CLEAR_ALL_NIGHT, CLEAR_EARLY, CLEARS_LATE, CLEARS_AGAIN, BRIEF_CLEAR, CLOUDY_ALL_NIGHT")
		String pattern,
		@Schema(description = "주의 사항. CLEARS_LATE, BRIGHT_MOON, CITY_LIGHTS, DEW")
		List<String> notes,
		@Schema(description = "추천 시간대. 맑은 시간이 없으면 빠진다")
		Window bestWindow,
		@Schema(description = "보름달을 1로 본 그 밤의 달 밝기", example = "0.09")
		double moonBrightness,
		@Schema(description = "그 밤의 달 모양과 뜨고 지는 시각")
		Moon moon,
		@Schema(description = "해가 진 뒤부터 뜨기 전까지의 시간별 조건. 오늘 밤은 이미 지난 시간이 빠진다")
		List<Hour> hours
	) {
		static Night of(NightAssessment night, ZoneId zone) {
			return new Night(
				night.date().toString(),
				night.level().name(),
				night.pattern().name(),
				night.notes().stream().map(Enum::name).toList(),
				night.bestWindow() != null ? Window.of(night.bestWindow(), zone) : null,
				NightRecommendationResponse.Place.moonBrightness(night),
				Moon.of(night.moon(), zone),
				night.hours().stream().map(hour -> Hour.of(hour, zone)).toList()
			);
		}
	}
}
