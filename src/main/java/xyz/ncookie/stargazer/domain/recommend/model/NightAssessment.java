package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 한 장소의 하룻밤 평가.
 *
 * @param date          밤이 시작하는 날짜
 * @param hours         해가 진 뒤부터 뜨기 전까지의 시간 칸. 예보가 있는 칸만 들어 있다
 * @param clearWindows  맑은 구간 전부. 시간순
 * @param bestWindow    추천하는 구간. 맑은 구간이 없으면 null
 * @param level         추천 등급
 * @param pattern       밤사이 하늘이 바뀌는 모양
 * @param notes         주의 사항
 * @param quality       추천 구간의 질(0~100). 같은 등급 안에서 순서를 정할 때 쓴다
 */
public record NightAssessment(
	LocalDate date,
	List<NightHour> hours,
	List<ClearWindow> clearWindows,
	ClearWindow bestWindow,
	RecommendationLevel level,
	NightPattern pattern,
	List<NightNote> notes,
	double quality
) {

	public NightAssessment {
		hours = List.copyOf(hours);
		clearWindows = List.copyOf(clearWindows);
		notes = List.copyOf(notes);
	}
}
