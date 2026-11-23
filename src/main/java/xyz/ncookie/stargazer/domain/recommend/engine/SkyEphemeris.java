package xyz.ncookie.stargazer.domain.recommend.engine;

import java.time.Instant;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPosition;
import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Component;

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
}
