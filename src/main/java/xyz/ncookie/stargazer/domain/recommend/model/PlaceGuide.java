package xyz.ncookie.stargazer.domain.recommend.model;

import java.util.List;

/**
 * 명소에 붙은 안내. 평가에는 쓰지 않고 화면에 보여 주기만 한다. 값마다 없을 수 있다.
 *
 * @param viewpoint   실제로 차를 대고 보는 지점의 이름
 * @param description 어떤 곳인지
 * @param guide       가는 법과 주차
 * @param caution     계절 폐쇄, 조명, 통제 같은 주의 사항
 * @param links       가는 길과 서는 자리를 더 자세히 적은 글. 없으면 빈 목록
 * @param points      이 명소 안에서 서서 보는 자리들. 자리가 하나뿐이면 빈 목록
 * @param videos      이 명소에서 별을 본 유튜브 영상. 찾지 못했으면 빈 목록
 * @param reels       이 명소에서 별을 본 인스타그램 게시물. 없으면 빈 목록
 */
public record PlaceGuide(
	String viewpoint, String description, String guide, String caution,
	List<PlaceLink> links, List<PlacePoint> points, List<PlaceVideo> videos, List<PlaceReel> reels
) {

	public PlaceGuide {
		links = links != null ? List.copyOf(links) : List.of();
		points = points != null ? List.copyOf(points) : List.of();
		videos = videos != null ? List.copyOf(videos) : List.of();
		reels = reels != null ? List.copyOf(reels) : List.of();
	}
}
