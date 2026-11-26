package xyz.ncookie.stargazer.domain.geocode.client.kakao;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 로컬 API의 키워드 검색과 주소 검색 응답 가운데 쓰는 부분. 두 응답 모두 documents 배열이고,
 * 키워드 검색에만 장소 이름과 분류가 있다.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoLocalResponse(List<Document> documents) {

	/**
	 * @param placeName    장소 이름. 키워드 검색에만 있다
	 * @param categoryName 분류. "여행 > 관광,명소 > 전망대"처럼 큰 것부터 적혀 있다. 키워드 검색에만 있다
	 * @param addressName  지번 주소. 주소 검색에서는 찾은 주소나 행정구역의 전체 이름이다
	 * @param x            경도
	 * @param y            위도
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Document(
		@JsonProperty("place_name") String placeName,
		@JsonProperty("category_name") String categoryName,
		@JsonProperty("address_name") String addressName,
		String x,
		String y
	) {}
}
