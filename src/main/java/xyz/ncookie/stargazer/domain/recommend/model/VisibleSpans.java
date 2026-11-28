package xyz.ncookie.stargazer.domain.recommend.model;

import java.util.List;

/**
 * 하룻밤 가운데 하늘의 한 점(은하수 중심, 유성우의 복사점)이 보이는 구간. 날씨는 보지 않는다.
 *
 * @param spans    하늘이 완전히 어둡고 그 점이 기준 고도 위에 있는 구간. 시간순이고 비어 있지 않다
 * @param moonless spans 가운데 달이 지평선 아래에 있는 구간. 시간순이고, 그동안 달이 내내 떠 있으면 비어 있다
 */
public record VisibleSpans(List<TimeSpan> spans, List<TimeSpan> moonless) {

	public VisibleSpans {
		spans = List.copyOf(spans);
		moonless = List.copyOf(moonless);
	}
}
