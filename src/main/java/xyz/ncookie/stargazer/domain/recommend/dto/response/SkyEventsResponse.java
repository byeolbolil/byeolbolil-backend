package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse.Moon;
import xyz.ncookie.stargazer.domain.recommend.model.NightEvent;
import xyz.ncookie.stargazer.domain.recommend.model.NightMoon;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "오늘 밤부터 다가오는 천문 현상. 날씨는 보지 않는다")
public record SkyEventsResponse(

	@Schema(description = "오늘 밤의 날짜. 새벽 4시 전에는 전날이다", example = "2026-12-08")
	String startDate,

	@Schema(description = "요청한 위도. 보내지 않았으면 빠진다")
	Double latitude,

	@Schema(description = "요청한 경도. 보내지 않았으면 빠진다")
	Double longitude,

	@Schema(description = "보라고 권하는 밤의 날짜순. 목록은 한국천문연구원의 발표를 해마다 옮겨 적으므로, 발표 전의 다음 해 현상은 없다")
	List<Item> events
) {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "다가오는 현상 하나")
	public record Item(
		@Schema(description = "보라고 권하는 밤이 시작하는 날짜. 한국천문연구원이 권한 밤이다", example = "2026-12-14")
		String night,
		@Schema(description = "그 밤이 예보가 닿는 7일 안에 드는지. 참이면 그 날짜의 밤 추천을 열 수 있다")
		boolean withinForecast,
		@Schema(description = "현상. 위치를 보냈으면 그 자리에서 보이는 구간이 들어 있다")
		NightSkyViews.Event event,
		@Schema(description = "위치를 보냈을 때 그 자리에서 본 그 밤의 달. 밤 추천의 moon과 같다")
		Moon moon,
		@Schema(description = "값을 옮겨 온 자료")
		String source
	) {

		/** 위치를 모를 때 */
		public static Item of(SkyEvent event, boolean withinForecast, ZoneId zone) {
			return new Item(event.night().toString(), withinForecast, NightSkyViews.Event.of(event, zone), null, event.source());
		}

		/** 한 자리에서 본 현상 */
		public static Item located(NightEvent event, NightMoon moon, boolean withinForecast, ZoneId zone) {
			return new Item(
				event.event().night().toString(),
				withinForecast,
				NightSkyViews.Event.located(event, zone),
				Moon.of(moon, zone),
				event.event().source()
			);
		}
	}

	public static SkyEventsResponse of(LocalDate startDate, Double latitude, Double longitude, List<Item> events) {
		return new SkyEventsResponse(startDate.toString(), latitude, longitude, events);
	}
}
