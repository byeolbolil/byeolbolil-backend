package xyz.ncookie.stargazer.domain.skyevent.model;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * 날짜가 정해진 천문 현상 하나. src/main/resources/skyevents/sky_events.csv의 한 줄이다.
 *
 * @param id          줄마다 다른 이름. 주소와 화면이 현상을 가리킬 때 쓴다. 예: 2026-geminids
 * @param type        유성우인지 월식인지
 * @param name        화면에 적는 이름
 * @param night       보라고 권하는 밤이 시작하는 날짜. 극대 시각이 한국의 낮이면 한국천문연구원이 권한 밤이다
 * @param peak        유성우가 가장 많이 떨어질 것으로 예상되는 시각. 월식은 가장 깊이 가려지는 시각
 * @param radiantRa   유성우 복사점의 J2000 적경(°). 월식이면 null
 * @param radiantDec  유성우 복사점의 J2000 적위(°). 월식이면 null
 * @param zhr         이상적인 조건(복사점이 천정, 한계등급 6.5)에서 한 시간에 보일 유성의 수. 월식이면 null
 * @param eclipseKind 월식의 종류. 유성우면 null
 * @param start       월식이 시작하는 시각(반영식은 반영 시작, 그 밖에는 본영 시작). 유성우면 null
 * @param end         월식이 끝나는 시각. 유성우면 null
 * @param source      이 줄의 값을 옮겨 온 자료
 */
public record SkyEvent(
	String id,
	SkyEventType type,
	String name,
	LocalDate night,
	Instant peak,
	@Nullable Double radiantRa,
	@Nullable Double radiantDec,
	@Nullable Integer zhr,
	@Nullable EclipseKind eclipseKind,
	@Nullable Instant start,
	@Nullable Instant end,
	String source
) {

	public boolean hasRadiant() {
		return radiantRa != null && radiantDec != null;
	}

	public boolean hasEclipseSpan() {
		return start != null && end != null;
	}
}
