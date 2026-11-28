package xyz.ncookie.stargazer.domain.recommend.engine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.engine.SkyEphemeris.Crossings;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;
import xyz.ncookie.stargazer.domain.recommend.model.VisibleSpans;

/**
 * 하늘의 고정된 한 점(은하수 중심, 유성우의 복사점)이 그 밤에 언제 보이는지 계산한다. 날씨는 보지 않는다.
 *
 * 보이는 구간은 하늘이 완전히 어둡고(태양 고도 NightAssessmentEngine.FULLY_DARK_SUN_ALTITUDE 이하) 그 점이
 * MIN_ALTITUDE보다 높이 있는 구간이다. 은하수 같은 퍼진 대상은 천문박명이 끝나야 보인다(docs/SCORING.md "어둠").
 * 밤의 범위는 달력과 같이 그날 정오부터 다음 날 정오다.
 */
@Component
@RequiredArgsConstructor
public class TargetVisibility {

	/**
	 * 정책: 하늘의 한 점을 "보인다"고 할 기준 고도(°). 은하수 중심이 보이기 시작하는 고도를 정한 출처는 찾지 못했다.
	 * docs/SCORING.md "눈으로 보이는 별 개수"가 고도 10° 위를 보이는 하늘로 잡고 있어서 같은 값을 쓴다.
	 * 지평선 근처는 대기를 길게 지나 흐려지고 산과 나무에 가리기 쉽다는 것 외에 크기를 정할 근거는 없다.
	 */
	public static final double MIN_ALTITUDE = 10.0;

	private static final Duration ONE_DAY = Duration.ofDays(1);

	private final SkyEphemeris skyEphemeris;

	/**
	 * @param rightAscension J2000 적경(°). 세차는 넣지 않는다. J2000에서 2027년까지 쌓인 세차는 0.4°쯤이고
	 *                       (Meeus 식 21.1의 어림식으로 궁수자리 A*의 적경이 연 3.8초), 기준 고도를 지나는 시각이 2분 안쪽으로 달라진다
	 * @param declination    J2000 적위(°)
	 * @return 그 밤에 한 번도 보이지 않으면 비어 있다
	 */
	public Optional<VisibleSpans> night(
		double rightAscension, double declination, double latitude, double longitude, LocalDate date, ZoneId zone
	) {
		Instant noon = ZonedDateTime.of(date, LocalTime.NOON, zone).toInstant();
		Instant nextNoon = noon.plus(ONE_DAY);

		Crossings sun = skyEphemeris.sunCrossings(
			latitude, longitude, noon, ONE_DAY, NightAssessmentEngine.FULLY_DARK_SUN_ALTITUDE);
		List<TimeSpan> dark = MoonCalendar.spansBelow(noon, nextNoon, sun);
		List<TimeSpan> above = spansAbove(rightAscension, declination, latitude, longitude, noon, nextNoon, MIN_ALTITUDE);
		List<TimeSpan> visible = intersect(dark, above);
		if (visible.isEmpty()) {
			return Optional.empty();
		}

		// 달은 월출, 월몰을 화면에 적는 것과 같은 범위에서 구한다(MoonCalendar.night와 같은 이유)
		Crossings moon = skyEphemeris.moonCrossings(latitude, longitude, noon, ONE_DAY);
		List<TimeSpan> moonDown = MoonCalendar.spansBelow(noon, nextNoon, moon);
		return Optional.of(new VisibleSpans(visible, intersect(visible, moonDown)));
	}

	/**
	 * 구간 안에서 달이 지평선 위에 있는 부분. 월식이 그 자리에서 보이는지를 가리는 데 쓴다.
	 */
	public List<TimeSpan> moonUp(double latitude, double longitude, TimeSpan span) {
		Crossings moon = skyEphemeris.moonCrossings(latitude, longitude, span.start(), span.duration());
		List<TimeSpan> down = MoonCalendar.spansBelow(span.start(), span.end(), moon);
		return subtract(span, down);
	}

	/**
	 * from부터 to 사이에서 그 점이 altitude보다 높이 있는 구간.
	 *
	 * 고정된 점의 고도는 시간각으로만 정해지고, 시간각은 항성시와 함께 일정하게 돈다. 그래서 기준 고도에 오는 시간각
	 * ±H0(Meeus 식 15.1)를 구하면 시간각이 그 안에 드는 구간을 바로 셈할 수 있다. 하루 동안 시간각은 361°쯤 돈다.
	 */
	static List<TimeSpan> spansAbove(
		double rightAscension, double declination, double latitude, double longitude, Instant from, Instant to, double altitude
	) {
		double h0 = HorizontalCoordinates.hourAngleAtAltitude(declination, latitude, altitude);
		List<TimeSpan> spans = new ArrayList<>();
		if (h0 <= 0.0) {
			return spans;
		}
		if (h0 >= 180.0) {
			spans.add(new TimeSpan(from, to));
			return spans;
		}

		double start = HorizontalCoordinates.hourAngle(rightAscension, longitude, from);
		double sweep = Duration.between(from, to).toNanos() / 1e9 / 86400.0 * HorizontalCoordinates.SIDEREAL_DEGREES_PER_DAY;
		// start는 -180 초과 180 이하, 끝은 start + sweep. 그 사이에 드는 [-h0 + 360k, h0 + 360k]를 모두 찾는다
		for (int k = 0; k <= (int) Math.ceil((start + sweep) / 360.0) + 1; k++) {
			double lo = Math.max(0.0, -h0 + 360.0 * k - start);
			double hi = Math.min(sweep, h0 + 360.0 * k - start);
			if (lo < hi) {
				spans.add(new TimeSpan(
					from.plus(HorizontalCoordinates.siderealDuration(lo)),
					from.plus(HorizontalCoordinates.siderealDuration(hi))));
			}
		}
		return spans;
	}

	/** 두 구간 목록이 겹치는 부분. 둘 다 시간순이면 결과도 시간순이다 */
	static List<TimeSpan> intersect(List<TimeSpan> a, List<TimeSpan> b) {
		List<TimeSpan> result = new ArrayList<>();
		for (TimeSpan x : a) {
			for (TimeSpan y : b) {
				Instant start = x.start().isAfter(y.start()) ? x.start() : y.start();
				Instant end = x.end().isBefore(y.end()) ? x.end() : y.end();
				if (start.isBefore(end)) {
					result.add(new TimeSpan(start, end));
				}
			}
		}
		result.sort((p, q) -> p.start().compareTo(q.start()));
		return result;
	}

	/** span에서 시간순인 구간들을 뺀 나머지 */
	private static List<TimeSpan> subtract(TimeSpan span, List<TimeSpan> holes) {
		List<TimeSpan> result = new ArrayList<>();
		Instant cursor = span.start();
		for (TimeSpan hole : holes) {
			if (hole.start().isAfter(cursor)) {
				result.add(new TimeSpan(cursor, hole.start()));
			}
			if (hole.end().isAfter(cursor)) {
				cursor = hole.end();
			}
		}
		if (cursor.isBefore(span.end())) {
			result.add(new TimeSpan(cursor, span.end()));
		}
		return result;
	}
}
