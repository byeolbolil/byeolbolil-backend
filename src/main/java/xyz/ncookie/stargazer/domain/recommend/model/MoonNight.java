package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 달력의 하룻밤. 예보 없이 해와 달의 위치만으로 정해진다.
 *
 * @param date     밤이 시작하는 날짜
 * @param moon     그 밤의 달 모양과 뜨고 지는 시각
 * @param dark     그날 정오부터 다음 날 정오 사이에 하늘이 어두운 구간. 그만큼 어두워지지 않는 밤이면 null
 * @param moonless 어두운 구간 가운데 달이 지평선 아래에 있는 구간. 시간순이고, 달이 밤새 떠 있으면 비어 있다
 */
public record MoonNight(LocalDate date, NightMoon moon, TimeSpan dark, List<TimeSpan> moonless) {

	public MoonNight {
		moonless = List.copyOf(moonless);
	}
}
