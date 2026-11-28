package xyz.ncookie.stargazer.domain.skyevent.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;
import xyz.ncookie.stargazer.domain.skyevent.provider.SkyEventCatalog;

/**
 * 날짜가 정해진 천문 현상. 밤 추천이 밤마다 어떤 현상이 있는지 찾을 때 쓴다.
 */
@Service
@RequiredArgsConstructor
public class SkyEventDomainService {

	private final SkyEventCatalog skyEventCatalog;

	/**
	 * 보라고 권하는 밤이 from 이상 to 미만인 현상. 밤의 날짜순이다.
	 */
	public List<SkyEvent> findBetween(LocalDate from, LocalDate to) {
		return skyEventCatalog.all().stream()
			.filter(event -> !event.night().isBefore(from) && event.night().isBefore(to))
			.toList();
	}

	/**
	 * 보라고 권하는 밤이 from 이후인 현상 전부. 밤의 날짜순이다.
	 */
	public List<SkyEvent> findFrom(LocalDate from) {
		return skyEventCatalog.all().stream()
			.filter(event -> !event.night().isBefore(from))
			.toList();
	}
}
