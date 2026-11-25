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
}
