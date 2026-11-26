package xyz.ncookie.stargazer.domain.spot.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "관측지 정보 응답 DTO")
public record ObservationSpotResponse(
	@Schema(description = "관측지 ID", example = "1")
	Long id,

	@Schema(description = "관측지 제목", example = "강릉 안반데기")
	String title,

	@Schema(description = "종류. OPEN_SITE는 밤에 자유롭게 가는 관측지, FACILITY는 프로그램으로 망원경을 보는 천문대와 과학관")
	String kind,

	@Schema(description = "실제로 차를 대고 보는 지점. 좌표가 가리키는 곳이다", example = "멍에전망대 주차장")
	String viewpoint,
	
	@Schema(description = "주소", example = "강원도 평창군 대관령면")
	String address,

	@Schema(description = "위도", example = "37.5665")
	Double latitude,
	
	@Schema(description = "경도", example = "126.9780")
	Double longitude,

	@Schema(description = "어떤 곳인지 한두 문장")
	String description,

	@Schema(description = "가는 법과 주차")
	String guide,

	@Schema(description = "계절 폐쇄, 조명, 통제 같은 주의 사항. 없으면 빠진다")
	String caution,

	@Schema(description = "가는 길과 서는 자리를 더 자세히 적은 글. 시설이면 공식 페이지. 없으면 빠진다")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Link> links,

	@Schema(description = "이 명소 안에서 서서 보는 자리들. 첫 자리가 차를 대는 곳이다. 자리가 하나뿐이면 빠진다")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Point> points,

	@Schema(description = "이 명소에서 별을 본 유튜브 영상. 찾지 못한 곳에서는 빠진다")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<Video> videos,

	@Schema(description = "보틀 등급 (1-9, 낮을수록 좋음)", example = "3")
	Integer bortleScale,

	@Schema(description = "주차 가능 여부", example = "true")
	Boolean isParkingAvailable,
	
	@Schema(description = "화장실 이용 가능 여부", example = "true")
	Boolean isRestroomAvailable,
	
	@Schema(description = "차량 접근 가능 여부", example = "true")
	Boolean isCarAccess,

	@Schema(description = "썸네일 이미지 URL", example = "https://example.com/image.jpg")
	String thumbnailImage
) {

	public static ObservationSpotResponse from(ObservationSpot spot) {
		return ObservationSpotResponse.builder()
			.id(spot.getId())
			.title(spot.getTitle())
			.kind(spot.getKind().name())
			.viewpoint(spot.getViewpoint())
			.address(spot.getAddress())
			.latitude(spot.getLatitude())
			.longitude(spot.getLongitude())
			.description(spot.getDescription())
			.guide(spot.getGuide())
			.caution(spot.getCaution())
			.links(spot.getLinks().stream().map(link -> new Link(link.getTitle(), link.getUrl(), link.getImageUrl())).toList())
			.points(spot.getPoints().stream()
				.map(point -> new Point(point.getName(), point.getAccess(), point.getLatitude(), point.getLongitude()))
				.toList())
			.videos(spot.getVideos().stream()
				.map(video -> new Video(
					video.getTitle(), video.getChannel(), video.getUrl(), video.getThumbnailUrl(), video.isShortForm()))
				.toList())
			.bortleScale(spot.getLightPollutionLevel())
			.isParkingAvailable(spot.getIsParkingAvailable())
			.isRestroomAvailable(spot.getIsRestroomAvailable())
			.isCarAccess(spot.getIsCarAccess())
			.thumbnailImage(spot.getThumbnailImage())
			.build();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "참고 링크")
	public record Link(
		@Schema(description = "그 글의 제목", example = "울산바위 은하수 포인트 가는법 정리")
		String title,
		String url,
		@Schema(description = "그 글의 대표 이미지. 링크의 미리보기로 쓴다. 없으면 빠진다")
		String imageUrl
	) {}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "명소 안에서 서서 보는 자리")
	public record Point(
		@Schema(description = "자리의 이름", example = "별빛언덕")
		String name,
		@Schema(description = "어떻게 가는지와 어떤 자리인지", example = "주차장에서 걸어서 20분쯤이에요.")
		String access,
		@Schema(description = "좌표를 아는 자리에만 있다")
		Double latitude,
		Double longitude
	) {}

	@Schema(description = "명소에서 별을 본 유튜브 영상")
	public record Video(
		@Schema(description = "영상 제목", example = "4K) 안반데기 3월 은하수 타임랩스")
		String title,
		@Schema(description = "올린 채널의 이름")
		String channel,
		@Schema(description = "영상을 여는 주소", example = "https://www.youtube.com/watch?v=xEfv8BDCpgw")
		String url,
		@Schema(description = "미리보기 그림(320×180)의 주소", example = "https://i.ytimg.com/vi/xEfv8BDCpgw/mqdefault.jpg")
		String thumbnailUrl,
		@Schema(description = "쇼츠(세로로 찍은 짧은 영상)인지")
		boolean shortForm
	) {}
}
