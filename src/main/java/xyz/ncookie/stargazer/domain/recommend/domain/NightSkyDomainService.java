package xyz.ncookie.stargazer.domain.recommend.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.engine.MilkyWay;
import xyz.ncookie.stargazer.domain.recommend.engine.SkyEphemeris;
import xyz.ncookie.stargazer.domain.recommend.engine.TargetVisibility;
import xyz.ncookie.stargazer.domain.recommend.model.NightEvent;
import xyz.ncookie.stargazer.domain.recommend.model.NightMoon;
import xyz.ncookie.stargazer.domain.recommend.model.NightSky;
import xyz.ncookie.stargazer.domain.recommend.model.TimeSpan;
import xyz.ncookie.stargazer.domain.recommend.model.VisibleSpans;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;

/**
 * 한 자리에서 본 밤하늘 가운데 날씨와 상관없는 것: 은하수 중심이 보이는 구간과 천문 현상이 보이는 구간.
 * 등급과 점수에는 넣지 않는다. 은하수가 뜨지 않는 밤도 별 보기에는 좋은 밤이고, 등급이 철마다 통째로 오르내리면
 * 그 밤에 어디가 좋은지를 견줄 수 없게 된다(docs/decisions.md 2026-11-27 "은하수가 보이는 시간과 천문 현상").
 */
@Service
@RequiredArgsConstructor
public class NightSkyDomainService {

	/** 정책: 다음에 은하수 중심이 보이는 밤을 찾는 범위. 한 해면 어느 자리에서든 다시 뜨는 철이 들어온다 */
	static final int MILKY_WAY_SEARCH_DAYS = 366;

	private final TargetVisibility targetVisibility;
	private final SkyEphemeris skyEphemeris;

	/**
	 * @param events 그 밤을 보라고 권하는 현상. 다른 밤의 현상은 넣지 않는다
	 */
	public NightSky nightSky(double latitude, double longitude, LocalDate date, ZoneId zone, List<SkyEvent> events) {
		return new NightSky(
			date,
			milkyWay(latitude, longitude, date, zone).orElse(null),
			events.stream().map(event -> nightEvent(event, latitude, longitude, zone)).toList()
		);
	}

	/** 그 자리에서 본 그 밤의 달 모양과 뜨고 지는 시각. 밤 추천의 달과 같은 기준이다 */
	public NightMoon nightMoon(double latitude, double longitude, LocalDate date, ZoneId zone) {
		return skyEphemeris.nightMoon(latitude, longitude, date, zone);
	}

	/** 그 밤에 은하수 중심이 보이는 구간. 뜨지 않으면 비어 있다 */
	public Optional<VisibleSpans> milkyWay(double latitude, double longitude, LocalDate date, ZoneId zone) {
		return targetVisibility.night(MilkyWay.CENTER_RA, MilkyWay.CENTER_DEC, latitude, longitude, date, zone);
	}

	/**
	 * from부터 처음으로 은하수 중심이 보이는 밤. 겨울에 언제부터 다시 보이는지 알려 주는 데 쓴다.
	 *
	 * @return 한 해 안에 한 번도 보이지 않는 자리면 비어 있다
	 */
	public Optional<LocalDate> nextMilkyWayNight(double latitude, double longitude, LocalDate from, ZoneId zone) {
		return from.datesUntil(from.plusDays(MILKY_WAY_SEARCH_DAYS))
			.filter(date -> milkyWay(latitude, longitude, date, zone).isPresent())
			.findFirst();
	}

	/**
	 * 그 자리에서 본 현상 하나. 유성우는 그 밤에 복사점이 보이는 구간을, 월식은 월식 동안 달이 떠 있는 구간을 낸다.
	 */
	public NightEvent nightEvent(SkyEvent event, double latitude, double longitude, ZoneId zone) {
		VisibleSpans radiant = event.hasRadiant()
			? targetVisibility.night(
				event.radiantRa(), event.radiantDec(), latitude, longitude, event.night(), zone).orElse(null)
			: null;
		List<TimeSpan> eclipse = event.hasEclipseSpan()
			? targetVisibility.moonUp(latitude, longitude, new TimeSpan(event.start(), event.end()))
			: List.of();
		return new NightEvent(event, radiant, eclipse);
	}
}
