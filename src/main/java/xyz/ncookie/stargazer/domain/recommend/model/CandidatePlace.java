package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 추천 후보 장소. 명소와 북마크를 같은 모양으로 다루기 위한 값이다.
 *
 * @param type   명소인지 내 장소인지
 * @param id     type이 SPOT이면 명소 ID, BOOKMARK면 북마크 ID
 * @param spotId 명소이거나 명소를 저장한 북마크라면 그 명소의 ID. 같은 명소가 두 번 나오지 않게 하는 데 쓴다
 * @param guide  명소의 안내. 직접 찍은 위치를 저장한 내 장소에는 없다(null)
 */
public record CandidatePlace(
	PlaceType type,
	Long id,
	Long spotId,
	String name,
	String address,
	double latitude,
	double longitude,
	PlaceGuide guide
) {

	public CandidatePlace(
		PlaceType type, Long id, Long spotId, String name, String address, double latitude, double longitude
	) {
		this(type, id, spotId, name, address, latitude, longitude, null);
	}
}
