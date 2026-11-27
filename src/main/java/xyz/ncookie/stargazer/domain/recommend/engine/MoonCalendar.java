package xyz.ncookie.stargazer.domain.recommend.engine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.engine.SkyEphemeris.Crossings;
import xyz.ncookie.stargazer.domain.recommend.model.MoonNight;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;

/**
 * 예보가 닿지 않는 날까지 밤마다 달이 언제 없는지 계산한다. 날씨는 보지 않는다.
 *
 * 하늘이 어두운 구간은 밤 추천과 같은 기준(태양 고도 NightAssessmentEngine.DARK_SUN_ALTITUDE 이하)으로 정해서,
 * 같은 밤을 두 화면이 다르게 말하지 않게 한다. 달은 밝기와 상관없이 지평선 위에 있으면 있는 것으로 본다.
 * 가는 초승달도 빼지 않는 것은, 달이 얼마나 방해되는지는 달 모양으로 따로 보여 주기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class MoonCalendar {

	private static final Duration ONE_DAY = Duration.ofDays(1);

	private final SkyEphemeris skyEphemeris;

	/**
	 * @param from 첫 밤의 날짜
	 * @param days 밤의 수
	 */
	public List<MoonNight> nights(double latitude, double longitude, LocalDate from, int days, ZoneId zone) {
		return from.datesUntil(from.plusDays(days))
			.map(date -> night(latitude, longitude, date, zone))
			.toList();
	}

	MoonNight night(double latitude, double longitude, LocalDate date, ZoneId zone) {
		Instant noon = ZonedDateTime.of(date, LocalTime.NOON, zone).toInstant();
		Crossings sun = skyEphemeris.sunCrossings(
			latitude, longitude, noon, ONE_DAY, NightAssessmentEngine.DARK_SUN_ALTITUDE);
		List<TimeSpan> darkSpans = spansBelow(noon, noon.plus(ONE_DAY), sun);

		// 달은 화면에 적는 월출, 월몰과 같은 범위(정오부터 정오)에서 구해야 구간의 끝이 그 시각과 분까지 맞는다.
		// 달이 다시 뜨기까지는 하루보다 길어서, 이 범위의 첫 월출과 첫 월몰 말고는 밤 안에 들 수 있는 것이 없다
		Crossings moon = skyEphemeris.moonCrossings(latitude, longitude, noon, ONE_DAY);
		List<TimeSpan> moonDown = spansBelow(noon, noon.plus(ONE_DAY), moon);

		List<TimeSpan> moonless = new ArrayList<>();
		for (TimeSpan dark : darkSpans) {
			for (TimeSpan down : moonDown) {
				addIfNotEmpty(moonless, later(dark.start(), down.start()), earlier(dark.end(), down.end()));
			}
		}

		// 정오에서 정오 사이에 어두운 구간이 둘로 갈리는 것은 극지방뿐이다. 그때는 처음부터 끝까지를 밤으로 본다
		TimeSpan dark = darkSpans.isEmpty()
			? null
			: new TimeSpan(darkSpans.get(0).start(), darkSpans.get(darkSpans.size() - 1).end());
		return new MoonNight(date, skyEphemeris.nightMoon(latitude, longitude, date, zone), dark, moonless);
	}

	/**
	 * from부터 to 사이에서 기준선 아래에 있는 구간. 처음 떠오르고 처음 지는 시각만으로 나눈다.
	 * 구간은 하루를 넘지 않는다. 해는 하루에 한 번, 달은 평균 24시간 50분에 한 번 뜨므로 그 안에서 두 번 떠오르는 일은 없다.
	 */
	static List<TimeSpan> spansBelow(Instant from, Instant to, Crossings crossings) {
		Instant rise = crossings.rise();
		Instant set = crossings.set();
		List<TimeSpan> spans = new ArrayList<>();

		if (crossings.downAtStart()) {
			// 아래에서 시작해 떠오르고, 그 뒤에 지면 끝까지 다시 아래에 있다
			addIfNotEmpty(spans, from, (rise != null) ? rise : to);
			if (rise != null && set != null && set.isAfter(rise)) {
				addIfNotEmpty(spans, set, to);
			}
		} else if (set != null) {
			// 위에서 시작해 지고, 그 뒤에 떠오르면 거기서 끝난다
			addIfNotEmpty(spans, set, (rise != null && rise.isAfter(set)) ? rise : to);
		}
		return spans;
	}

	private static Instant later(Instant a, Instant b) {
		return a.isAfter(b) ? a : b;
	}

	private static Instant earlier(Instant a, Instant b) {
		return a.isBefore(b) ? a : b;
	}

	private static void addIfNotEmpty(List<TimeSpan> spans, Instant start, Instant end) {
		if (start.isBefore(end)) {
			spans.add(new TimeSpan(start, end));
		}
	}
}
