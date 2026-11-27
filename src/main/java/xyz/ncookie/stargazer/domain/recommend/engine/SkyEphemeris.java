package xyz.ncookie.stargazer.domain.recommend.engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPosition;
import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.domain.recommend.model.NightMoon;

/**
 * 한 시각의 해와 달의 위치. commons-suncalc 계산을 감싼다.
 */
@Component
public class SkyEphemeris {

	/**
	 * 태양 중심의 기하학적 고도(°). 박명의 정의가 대기 굴절을 뺀 이 값을 기준으로 한다.
	 * U.S. Naval Observatory, Rise, Set, and Twilight Definitions, https://aa.usno.navy.mil/faq/RST_defs
	 */
	public double sunAltitude(double latitude, double longitude, Instant time) {
		return SunPosition.compute().at(latitude, longitude).on(time).execute().getTrueAltitude();
	}

	/** 달의 고도(°). 0보다 크면 지평선 위에 있다 */
	public double moonAltitude(double latitude, double longitude, Instant time) {
		return MoonPosition.compute().at(latitude, longitude).on(time).execute().getAltitude();
	}

	/** 달의 위상각(°). 보름일 때 0, 삭일 때 180 */
	public double moonPhaseAngle(Instant time) {
		return Math.abs(MoonIllumination.compute().on(time).execute().getPhase());
	}

	/** 달에서 햇빛을 받아 밝게 보이는 면의 비율. 삭일 때 0, 보름일 때 1 */
	public double moonIlluminatedFraction(Instant time) {
		return MoonIllumination.compute().on(time).execute().getFraction();
	}

	/**
	 * 달이 차오르는 중인지. 삭에서 보름으로 가는 동안 참이다.
	 * commons-suncalc의 위상각은 삭이 -180°, 상현이 -90°, 보름이 0°, 하현이 90°다.
	 */
	public boolean isMoonWaxing(Instant time) {
		return MoonIllumination.compute().on(time).execute().getPhase() < 0;
	}

	/** from부터 24시간 안에 달이 처음 뜨는 시각. 그 사이에 뜨지 않으면 비어 있다 */
	public Optional<Instant> moonrise(double latitude, double longitude, Instant from) {
		return Optional.ofNullable(moonTimes(latitude, longitude, from).getRise()).map(ZonedDateTime::toInstant);
	}

	/** from부터 24시간 안에 달이 처음 지는 시각. 그 사이에 지지 않으면 비어 있다 */
	public Optional<Instant> moonset(double latitude, double longitude, Instant from) {
		return Optional.ofNullable(moonTimes(latitude, longitude, from).getSet()).map(ZonedDateTime::toInstant);
	}

	private static MoonTimes moonTimes(double latitude, double longitude, Instant from) {
		return MoonTimes.compute().at(latitude, longitude).on(from).oneDay().execute();
	}

	/**
	 * 그 밤의 달. 모양은 자정의 값으로 정한다. 하룻밤 사이에 눈에 띄게 달라지지 않기 때문이다.
	 * 뜨고 지는 시각은 밤 시간 칸과 같은 범위(그날 정오부터 다음 날 정오)에서 찾는다.
	 */
	public NightMoon nightMoon(double latitude, double longitude, LocalDate date, ZoneId zone) {
		Instant noon = ZonedDateTime.of(date, LocalTime.NOON, zone).toInstant();
		Instant midnight = noon.plus(12, ChronoUnit.HOURS);
		return new NightMoon(
			moonIlluminatedFraction(midnight),
			isMoonWaxing(midnight),
			moonrise(latitude, longitude, noon).orElse(null),
			moonset(latitude, longitude, noon).orElse(null)
		);
	}
}
