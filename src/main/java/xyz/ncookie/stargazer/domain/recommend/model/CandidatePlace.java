package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.LocalDate;

/**
 * 추천 후보 장소. 명소와 북마크를 같은 모양으로 다루기 위한 값이다.
 *
 * @param type   명소인지 내 장소인지
 * @param id     type이 SPOT이면 명소 ID, BOOKMARK면 북마크 ID
 * @param spotId 명소이거나 명소를 저장한 북마크라면 그 명소의 ID. 같은 명소가 두 번 나오지 않게 하는 데 쓴다
 * @param guide  명소의 안내. 직접 찍은 위치를 저장한 내 장소에는 없다(null)
 * @param lightsInView 지형 너머로 직접 보이는 먼 불빛의 지수. 미리 계산해 둔 명소에만 있다(없으면 null)
 * @param closed 길이 막혀 갈 수 없는 기간. 기본 추천 명소에만 넣는다. 직접 저장한 장소는 통제 기간에도 평가해서
 *               보여 주므로 null로 둔다
 */
public record CandidatePlace(
	PlaceType type,
	Long id,
	Long spotId,
	String name,
	String address,
	double latitude,
	double longitude,
	PlaceGuide guide,
	Double lightsInView,
	ClosedPeriod closed
) {

	public CandidatePlace(
		PlaceType type, Long id, Long spotId, String name, String address, double latitude, double longitude
	) {
		this(type, id, spotId, name, address, latitude, longitude, null, null, null);
	}

	/** 그 날짜에 시작하는 밤에 갈 수 없는 곳인지 */
	public boolean isClosedOn(LocalDate date) {
		return closed != null && closed.contains(date);
	}
}
