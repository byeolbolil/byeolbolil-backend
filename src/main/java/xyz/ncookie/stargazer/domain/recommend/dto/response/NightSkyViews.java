package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.recommend.dto.response.MoonCalendarResponse.Span;
import xyz.ncookie.stargazer.domain.recommend.model.NightEvent;
import xyz.ncookie.stargazer.domain.recommend.model.NightSky;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;
import xyz.ncookie.stargazer.domain.recommend.model.VisibleSpans;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;

/**
 * 밤 추천, 이번 주, 달력, 현상 목록이 함께 쓰는 은하수와 천문 현상 항목.
 */
public final class NightSkyViews {

	private NightSkyViews() {
	}

	/** 은하수가 뜨지 않는 밤이면 null */
	static MilkyWay milkyWayOf(NightSky sky, ZoneId zone) {
		return (sky != null && sky.milkyWay() != null) ? MilkyWay.of(sky.milkyWay(), zone) : null;
	}

	/** 그 자리에서 본 현상들. 현상이 없는 밤이면 빈 목록이다 */
	static List<Event> eventsOf(NightSky sky, ZoneId zone) {
		return (sky != null) ? sky.events().stream().map(event -> Event.located(event, zone)).toList() : List.of();
	}

	@Schema(description = "은하수 중심(궁수자리 A*)이 보이는 시간. 태양 고도 -18° 아래로 하늘이 완전히 어둡고 중심이 고도 10° 위에 "
		+ "있는 구간이다. 날씨는 보지 않는다. 그 밤에 뜨지 않으면 이 항목이 빠진다")
	public record MilkyWay(
		@Schema(description = "보이는 구간. 시간순이고 하나 이상이다")
		List<Span> visible,
		@Schema(description = "visible 가운데 달이 지평선 아래에 있는 구간. 그동안 달이 내내 떠 있으면 빈 배열이다")
		List<Span> moonless
	) {
		static MilkyWay of(VisibleSpans spans, ZoneId zone) {
			return new MilkyWay(spansOf(spans.spans(), zone), spansOf(spans.moonless(), zone));
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "천문 현상 하나. 유성우와 월식이다(한국천문연구원 \"주목할 천문현상\")")
	public record Event(
		@Schema(description = "현상마다 다른 이름", example = "2026-geminids")
		String id,
		@Schema(description = "METEOR_SHOWER 또는 LUNAR_ECLIPSE")
		String type,
		@Schema(description = "화면에 적는 이름", example = "쌍둥이자리 유성우")
		String name,
		@Schema(description = "유성우는 극대 예상 시각, 월식은 가장 깊이 가려지는 시각", example = "2026-12-14T23:00:00+09:00")
		String peak,
		@Schema(description = "유성우만. 이상적인 조건(복사점이 천정, 한계등급 6.5)에서 한 시간에 보일 유성의 수(IMO). "
			+ "실제로 보이는 수는 이보다 훨씬 적다", example = "150")
		Integer zhr,
		@Schema(description = "월식만. TOTAL, PARTIAL, PENUMBRAL")
		String eclipseKind,
		@Schema(description = "월식만. 반영월식은 반영식, 그 밖에는 부분식이 시작하는 시각")
		String start,
		@Schema(description = "월식만. 같은 기준으로 끝나는 시각")
		String end,
		@Schema(description = "자리를 알 때만. 유성우는 하늘이 완전히 어둡고 복사점이 고도 10° 위에 있는 구간, 월식은 월식 가운데 "
			+ "달이 지평선 위에 있는 구간. 그 자리에서 보이지 않으면 빈 배열이다")
		List<Span> visible,
		@Schema(description = "자리를 알 때, 유성우만. visible 가운데 달이 지평선 아래에 있는 구간")
		List<Span> moonless
	) {

		/** 자리를 모를 때. 보이는 구간이 없다 */
		static Event of(SkyEvent event, ZoneId zone) {
			return new Event(
				event.id(),
				event.type().name(),
				event.name(),
				format(event.peak(), zone),
				event.zhr(),
				event.eclipseKind() != null ? event.eclipseKind().name() : null,
				event.start() != null ? format(event.start(), zone) : null,
				event.end() != null ? format(event.end(), zone) : null,
				null,
				null
			);
		}

		/** 한 자리에서 본 현상 */
		static Event located(NightEvent located, ZoneId zone) {
			Event base = of(located.event(), zone);
			boolean meteor = located.event().hasRadiant();
			List<Span> visible = meteor
				? (located.radiant() != null ? spansOf(located.radiant().spans(), zone) : List.of())
				: spansOf(located.eclipse(), zone);
			List<Span> moonless = meteor
				? (located.radiant() != null ? spansOf(located.radiant().moonless(), zone) : List.of())
				: null;
			return new Event(base.id, base.type, base.name, base.peak, base.zhr, base.eclipseKind, base.start, base.end,
				visible, moonless);
		}
	}

	private static List<Span> spansOf(List<TimeSpan> spans, ZoneId zone) {
		return spans.stream().map(span -> Span.of(span, zone)).toList();
	}

	private static String format(Instant time, ZoneId zone) {
		return NightRecommendationResponse.format(time.truncatedTo(ChronoUnit.MINUTES), zone);
	}
}
