package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.LocalDate;

/**
 * 길이 막히거나 주차장을 닫아서 갈 수 없는 기간. 겨울에 통제되는 고갯길과 고원이 해당한다.
 *
 * @param from  막히는 첫날
 * @param until 막히는 마지막 날
 */
public record ClosedPeriod(LocalDate from, LocalDate until) {

	/** 그 날짜에 시작하는 밤에 갈 수 없는지 */
	public boolean contains(LocalDate date) {
		return !date.isBefore(from) && !date.isAfter(until);
	}
}
