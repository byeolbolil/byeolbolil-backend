package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * 한 자리에서 본 그 밤의 하늘 가운데 날씨와 상관없이 정해지는 것. 등급과 점수에는 쓰지 않고 알려 주기만 한다.
 *
 * @param date     밤이 시작하는 날짜
 * @param milkyWay 은하수 중심이 보이는 구간. 그 밤에 뜨지 않으면 null
 * @param events   그 밤을 보라고 권하는 천문 현상. 없으면 비어 있다
 */
public record NightSky(LocalDate date, @Nullable VisibleSpans milkyWay, List<NightEvent> events) {

	public NightSky {
		events = List.copyOf(events);
	}
}
