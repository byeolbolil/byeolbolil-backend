package xyz.ncookie.stargazer.domain.recommend.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;

/**
 * 한 자리에서 본 천문 현상 하나.
 *
 * @param event   현상
 * @param radiant 유성우면 복사점이 보이는 구간. 그 밤에 복사점이 기준 고도 위로 오르지 않거나 월식이면 null
 * @param eclipse 월식이면 월식 가운데 달이 지평선 위에 있는 구간. 시간순이고, 그 자리에서 보이지 않으면 비어 있다.
 *                유성우면 비어 있다
 */
public record NightEvent(SkyEvent event, @Nullable VisibleSpans radiant, List<TimeSpan> eclipse) {

	public NightEvent {
		eclipse = List.copyOf(eclipse);
	}
}
