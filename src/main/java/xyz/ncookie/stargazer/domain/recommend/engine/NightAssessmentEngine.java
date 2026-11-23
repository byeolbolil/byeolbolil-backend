package xyz.ncookie.stargazer.domain.recommend.engine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.model.ClearWindow;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.NightHour;
import xyz.ncookie.stargazer.domain.recommend.model.NightNote;
import xyz.ncookie.stargazer.domain.recommend.model.NightPattern;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;

/**
 * 한 장소의 하룻밤을 시간별 예보로 평가한다.
 *
 * 한 시각의 점수가 아니라 "맑은 시간이 언제부터 얼마나 이어지는지"로 등급을 정한다.
 * 여기 있는 기준값의 근거와 신뢰도는 docs/SCORING.md에 있고, 값마다 같은 출처를 주석으로 달았다.
 * "정책"이라고 적은 값은 외부 근거 없이 별볼일이 정한 값이다.
 */
@Component
@RequiredArgsConstructor
public class NightAssessmentEngine {

	// ---- 어둠 ----

	// 이 고도보다 낮으면 밤으로 본다. 항해박명의 끝이다.
	// 정의: U.S. Naval Observatory, Rise, Set, and Twilight Definitions, https://aa.usno.navy.mil/faq/RST_defs
	// -12°면 밝은 별과 별자리를 보기에 충분하다는 판단: NOAA NWS, Twilight, https://www.weather.gov/fsd/twilight
	// (이 문서의 설명을 근거로 별볼일이 입문자 기준으로 적용한 것이다)
	static final double DARK_SUN_ALTITUDE = -12.0;

	// 이 고도보다 낮으면 하늘이 완전히 어둡다. 천문박명의 끝이고, 은하수 같은 퍼진 대상에 필요하다. 출처는 위와 같다.
	static final double FULLY_DARK_SUN_ALTITUDE = -18.0;

	// ---- 구름 ----

	// 구름이 이 값 이하인 시간만 관측 가능한 시간으로 센다.
	// Clear Sky Chart가 기후 통계에서 쓰는 기준이다. https://server1.cleardarksky.com/clmt/c/Dallasct.html
	static final int CLEAR_CLOUD_COVER_MAX = 30;

	// ---- 습도 ----

	// 이 값 이상이면 이슬 주의를 붙인다. 7Timer!의 첫 번째 습도 경고 단계다. https://www.7timer.info/doc.php?lang=en
	static final int DEW_HUMIDITY_MIN = 80;

	// ---- 달 ----

	// 정책: 보름 대비 밝기가 이 값 이상인 달이 추천 구간의 절반 이상 떠 있으면 등급을 한 단계 내린다.
	// 밝기 계산은 MoonBrightness(Krisciunas & Schaefer 1991)를 따르지만 0.25라는 경계는 근거 자료가 없다.
	static final double BRIGHT_MOON_MIN = 0.25;

	// ---- 광해 ----

	// 정책: Bortle 7등급(교외와 도시 사이) 이상이면 맑아도 "잠깐 볼 수 있어요"까지만 준다. 근거 자료가 없다.
	// 광해 기준을 연속값으로 바꾸면(로드맵 4단계) Falchi et al. 2016의 은하수 가시 한계로 대체할 값이다.
	static final int CITY_BORTLE_MIN = 7;

	// ---- 시간별 점수의 가중치 ----
	// 정책: 아래 세 값은 근거 자료가 없다. 요인마다 0~1로 나타낸 나쁜 정도에 곱해, 그만큼 점수를 깎는 비율이다.
	// 광해 0.5와 달 0.3은 이전 엔진의 최대 감점(50점, 30점)을 이어받았고, 박명 0.2는 새로 정했다.
	// 요인을 곱으로 묶은 것은 광해가 심한 곳에서는 달의 영향이 줄어든다는 관측과 맞추기 위해서다.
	// Puschnig, Wallner & Posch, MNRAS (2020), https://arxiv.org/abs/1912.05673
	static final double LIGHT_POLLUTION_WEIGHT = 0.5;
	static final double MOON_WEIGHT = 0.3;
	static final double TWILIGHT_WEIGHT = 0.2;

	// ---- 추천 구간 고르기 ----

	// 정책: 선호 시각 뒤의 맑은 시간은 절반으로 친다.
	static final double LATE_HOUR_WEIGHT = 0.5;

	// 정책: 추천 구간의 질을 매길 때 이 시간까지만 길이를 쳐준다. 그보다 길어도 더 좋다고 보지 않는다.
	static final int QUALITY_FULL_HOURS = 6;

	// 정책: 추천 구간이 이 시간 이상이면 "가볼 만해요", 그보다 짧으면 "잠깐 볼 수 있어요".
	static final int GOOD_MIN_HOURS = 2;

	// ---- 밤 패턴 ----

	// 정책: 어두운 시간 가운데 맑은 시간이 이 비율 이상이면 "밤새 맑아요"로 부른다. 근거 자료가 없다.
	static final double ALL_NIGHT_CLEAR_RATIO = 0.9;

	private static final Duration ONE_HOUR = Duration.ofHours(1);

	private final SkyEphemeris skyEphemeris;

	/**
	 * @param date        밤이 시작하는 날짜
	 * @param zone        date를 읽을 시간대
	 * @param bortleClass 장소의 광해 등급(1~9)
	 * @param forecast    시간별 예보. 그 밤의 시각이 빠져 있으면 그 칸은 평가에서 빠진다
	 * @return 그 밤의 어두운 시간에 예보가 하나도 없으면 비어 있다
	 */
	public Optional<NightAssessment> assess(
		double latitude,
		double longitude,
		LocalDate date,
		ZoneId zone,
		int bortleClass,
		List<HourlyWeather> forecast,
		RecommendationPolicy policy
	) {
		List<NightHour> hours = nightHours(latitude, longitude, date, zone, bortleClass, forecast);
		if (hours.stream().noneMatch(NightHour::dark)) {
			return Optional.empty();
		}

		Instant preferBefore = preferBeforeInstant(date, zone, policy.preferBefore());
		List<ClearWindow> windows = clearWindows(hours);
		ClearWindow best = bestWindow(windows, hours, preferBefore);

		RecommendationLevel level = RecommendationLevel.NO;
		List<NightNote> notes = new ArrayList<>();
		double quality = 0;

		if (best != null) {
			List<NightHour> bestHours = hours.stream().filter(hour -> best.contains(hour.time())).toList();
			long preferredHours = bestHours.stream().filter(hour -> endsBy(hour, preferBefore)).count();

			if (preferredHours >= policy.minClearHours()) {
				level = RecommendationLevel.STRONG;
			} else if (bestHours.size() >= GOOD_MIN_HOURS) {
				level = RecommendationLevel.GOOD;
			} else {
				level = RecommendationLevel.MAYBE;
			}

			if (preferredHours == 0) {
				notes.add(NightNote.CLEARS_LATE);
			}
			if (isMoonBright(bestHours)) {
				level = level.loweredOnce();
				notes.add(NightNote.BRIGHT_MOON);
			}
			if (bortleClass >= CITY_BORTLE_MIN && level.isAbove(RecommendationLevel.MAYBE)) {
				level = RecommendationLevel.MAYBE;
				notes.add(NightNote.CITY_LIGHTS);
			}
			if (bestHours.stream().anyMatch(hour -> hour.relativeHumidity() >= DEW_HUMIDITY_MIN)) {
				notes.add(NightNote.DEW);
			}

			quality = quality(bestHours, preferBefore);
		}

		return Optional.of(new NightAssessment(date, hours, windows, best, level, pattern(hours, windows), notes, quality));
	}

	/**
	 * 그날 정오부터 다음 날 정오까지의 시간 칸 가운데, 해가 지평선 아래에 있고 예보가 있는 칸.
	 */
	private List<NightHour> nightHours(
		double latitude, double longitude, LocalDate date, ZoneId zone, int bortleClass, List<HourlyWeather> forecast
	) {
		Map<Instant, HourlyWeather> weatherByTime = forecast.stream()
			.collect(Collectors.toMap(HourlyWeather::time, Function.identity(), (first, second) -> first));

		Instant noon = ZonedDateTime.of(date, LocalTime.NOON, zone).toInstant();
		List<NightHour> hours = new ArrayList<>();

		for (int i = 0; i < 24; i++) {
			Instant start = noon.plus(i, ChronoUnit.HOURS);
			Instant end = start.plus(ONE_HOUR);
			HourlyWeather weather = weatherByTime.get(start);
			if (weather == null) {
				continue;
			}

			// 저녁과 새벽에는 태양 고도가 한 방향으로만 움직이므로, 칸의 양 끝만 보면 칸 전체를 알 수 있다.
			double sunAltitude = Math.max(
				skyEphemeris.sunAltitude(latitude, longitude, start),
				skyEphemeris.sunAltitude(latitude, longitude, end)
			);
			if (sunAltitude >= 0) {
				continue;
			}

			Instant middle = start.plus(30, ChronoUnit.MINUTES);
			boolean dark = sunAltitude <= DARK_SUN_ALTITUDE;
			boolean moonUp = skyEphemeris.moonAltitude(latitude, longitude, middle) > 0;
			double moonBrightness = MoonBrightness.relativeToFull(skyEphemeris.moonPhaseAngle(middle));

			hours.add(new NightHour(
				start,
				dark,
				sunAltitude,
				weather.cloudCover(),
				weather.cloudCoverLow(),
				weather.cloudCoverMid(),
				weather.cloudCoverHigh(),
				weather.relativeHumidity(),
				moonUp,
				moonBrightness,
				dark && weather.cloudCover() <= CLEAR_CLOUD_COVER_MAX,
				dark ? score(weather.cloudCover(), bortleClass, moonUp, moonBrightness, sunAltitude) : 0
			));
		}
		return hours;
	}

	/**
	 * 어두운 칸의 점수. 100에서 시작해 요인마다 보이는 하늘의 비율을 곱해 나간다.
	 * 구름은 가려지지 않은 하늘의 비율을 그대로 쓰고, 나머지 요인은 정책 가중치만큼만 깎는다.
	 */
	static int score(int cloudCover, int bortleClass, boolean moonUp, double moonBrightness, double sunAltitude) {
		double cloud = 1 - clamp(cloudCover / 100.0);
		double lightPollution = 1 - LIGHT_POLLUTION_WEIGHT * clamp((bortleClass - 1) / 8.0);
		double moon = 1 - MOON_WEIGHT * (moonUp ? clamp(moonBrightness) : 0);
		double twilight = 1 - TWILIGHT_WEIGHT * clamp(
			(sunAltitude - FULLY_DARK_SUN_ALTITUDE) / (DARK_SUN_ALTITUDE - FULLY_DARK_SUN_ALTITUDE));

		return (int) Math.round(100 * cloud * lightPollution * moon * twilight);
	}

	private static List<ClearWindow> clearWindows(List<NightHour> hours) {
		List<ClearWindow> windows = new ArrayList<>();
		Instant start = null;
		Instant end = null;

		for (NightHour hour : hours) {
			boolean continues = end != null && hour.time().equals(end);
			if (hour.clear() && continues) {
				end = hour.time().plus(ONE_HOUR);
				continue;
			}
			if (start != null) {
				windows.add(new ClearWindow(start, end));
				start = null;
				end = null;
			}
			if (hour.clear()) {
				start = hour.time();
				end = hour.time().plus(ONE_HOUR);
			}
		}
		if (start != null) {
			windows.add(new ClearWindow(start, end));
		}
		return windows;
	}

	/**
	 * 점수의 합이 가장 큰 구간. 선호 시각 뒤의 시간은 절반만 친다. 합이 같으면 먼저 오는 구간이다.
	 */
	private static ClearWindow bestWindow(List<ClearWindow> windows, List<NightHour> hours, Instant preferBefore) {
		ClearWindow best = null;
		double bestValue = -1;
		for (ClearWindow window : windows) {
			double value = hours.stream()
				.filter(hour -> window.contains(hour.time()))
				.mapToDouble(hour -> hour.score() * weight(hour, preferBefore))
				.sum();
			if (value > bestValue) {
				bestValue = value;
				best = window;
			}
		}
		return best;
	}

	private static double quality(List<NightHour> bestHours, Instant preferBefore) {
		double weightedAverage = bestHours.stream()
			.mapToDouble(hour -> hour.score() * weight(hour, preferBefore))
			.average()
			.orElse(0);
		return weightedAverage * Math.min(bestHours.size(), QUALITY_FULL_HOURS) / QUALITY_FULL_HOURS;
	}

	private static double weight(NightHour hour, Instant preferBefore) {
		return endsBy(hour, preferBefore) ? 1.0 : LATE_HOUR_WEIGHT;
	}

	private static boolean endsBy(NightHour hour, Instant limit) {
		return !hour.time().plus(ONE_HOUR).isAfter(limit);
	}

	private static boolean isMoonBright(List<NightHour> bestHours) {
		long brightMoonHours = bestHours.stream()
			.filter(hour -> hour.moonUp() && hour.moonBrightness() >= BRIGHT_MOON_MIN)
			.count();
		return brightMoonHours > 0 && brightMoonHours * 2 >= bestHours.size();
	}

	/**
	 * 맑은 구간이 밤의 어디에 놓였는지로 이름을 붙인다.
	 * 구간이 하나일 때는 어두워지자마자 맑은지, 날이 밝을 때까지 맑은지만 보므로 따로 기준값이 없다.
	 */
	private static NightPattern pattern(List<NightHour> hours, List<ClearWindow> windows) {
		List<NightHour> dark = hours.stream().filter(NightHour::dark).toList();
		long clear = dark.stream().filter(NightHour::clear).count();

		if (clear == 0) {
			return NightPattern.CLOUDY_ALL_NIGHT;
		}
		if (clear >= dark.size() * ALL_NIGHT_CLEAR_RATIO) {
			return NightPattern.CLEAR_ALL_NIGHT;
		}
		if (windows.size() >= 2) {
			return NightPattern.CLEARS_AGAIN;
		}

		ClearWindow window = windows.get(0);
		Instant firstDark = dark.get(0).time();
		Instant lastDarkEnd = dark.get(dark.size() - 1).time().plus(ONE_HOUR);

		if (window.start().equals(firstDark)) {
			return NightPattern.CLEAR_EARLY;
		}
		if (window.end().equals(lastDarkEnd)) {
			return NightPattern.CLEARS_LATE;
		}
		return NightPattern.BRIEF_CLEAR;
	}

	/** 정오 전의 시각은 다음 날 새벽으로, 정오부터는 그날 저녁으로 읽는다. */
	private static Instant preferBeforeInstant(LocalDate date, ZoneId zone, LocalTime preferBefore) {
		LocalDate day = preferBefore.isBefore(LocalTime.NOON) ? date.plusDays(1) : date;
		return ZonedDateTime.of(day, preferBefore, zone).toInstant();
	}

	private static double clamp(double value) {
		return Math.max(0, Math.min(1, value));
	}
}
