package xyz.ncookie.stargazer.domain.geocode.client.kakao;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;

/**
 * 카카오 로컬 API 클라이언트. 전망대, 주차장, 공원처럼 OpenStreetMap에 빠진 국내 장소와 지번 주소를 찾는다.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide
 *
 * 키가 비어 있으면 쓰지 않는다. 실패는 모두 GeocodeProviderException으로 던진다.
 * 카카오는 응답을 저장해 두고 다시 쓰는 것을 허용하지 않으므로, 받은 결과는 캐시에 넣지 않고 그때그때 부른다.
 */
@Component
public class KakaoLocalClient {

	// 키워드 검색이 한 번에 주는 최대 개수다. 숙소와 가게가 섞여 오므로 넉넉히 받아 온 뒤 추린다
	private static final int KEYWORD_SIZE = 15;
	// 주소 검색은 동 이름 하나에 "1가, 2가"가 줄줄이 온다. 앞의 몇 개면 된다
	private static final int ADDRESS_SIZE = 5;

	// 목적지나 출발지가 될 만한 분류. 카카오의 분류 이름은 큰 것부터 " > "로 이어져 있다.
	// 관광지와 공원, 역과 주차장, 지명이 여기에 들고 숙소, 음식점, 가게, 관공서는 뒤로 보낸다. 정한 값이고 근거 자료는 없다
	private static final List<String> DESTINATION_CATEGORIES = List.of(
		"여행 > 관광,명소", "여행 > 공원", "교통,수송", "부동산 > 지명");

	private final RestTemplate restTemplate;
	private final String baseUrl;
	private final String restApiKey;

	public KakaoLocalClient(
		RestTemplate restTemplate,
		@Value("${geocode.kakao.base-url}") String baseUrl,
		@Value("${geocode.kakao.rest-api-key}") String restApiKey
	) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.restApiKey = restApiKey == null ? "" : restApiKey.strip();
	}

	public boolean isConfigured() {
		return !restApiKey.isEmpty();
	}

	/**
	 * 이름으로 장소를 찾는다. 카카오가 돌려준 순서를 그대로 둔다.
	 */
	public List<GeocodePlace> searchPlaces(String query) {
		List<GeocodePlace> places = new ArrayList<>();
		for (KakaoLocalResponse.Document document : get("/v2/local/search/keyword.json", query, KEYWORD_SIZE)) {
			if (isBlank(document.placeName()) || !hasPosition(document)) {
				continue;
			}
			places.add(new GeocodePlace(
				document.placeName().strip(),
				regionsOf(document.addressName()),
				kindOf(document.categoryName()),
				coordinate(document.y()),
				coordinate(document.x()),
				GeocodePlace.Source.KAKAO
			));
		}
		return places;
	}

	/**
	 * 지번이나 도로명 주소, 행정구역 이름으로 위치를 찾는다. 이름은 카카오가 정리한 전체 주소다.
	 */
	public List<GeocodePlace> searchAddresses(String query) {
		List<GeocodePlace> places = new ArrayList<>();
		for (KakaoLocalResponse.Document document : get("/v2/local/search/address.json", query, ADDRESS_SIZE)) {
			if (isBlank(document.addressName()) || !hasPosition(document)) {
				continue;
			}
			places.add(new GeocodePlace(
				document.addressName().strip(),
				List.of(),
				GeocodePlace.Kind.PLACE,
				coordinate(document.y()),
				coordinate(document.x()),
				GeocodePlace.Source.KAKAO
			));
		}
		return places;
	}

	private List<KakaoLocalResponse.Document> get(String path, String query, int size) {
		URI uri = UriComponentsBuilder.fromUriString(baseUrl + path)
			.queryParam("query", "{query}")
			.queryParam("size", size)
			.encode()
			.buildAndExpand(query)
			.toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey);

		KakaoLocalResponse body;
		try {
			body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), KakaoLocalResponse.class)
				.getBody();
		} catch (RestClientException e) {
			throw new GeocodeProviderException("카카오 로컬 호출 실패: " + e.getMessage(), e);
		}
		if (body == null || body.documents() == null) {
			throw new GeocodeProviderException("카카오 로컬이 빈 응답을 돌려줬습니다.");
		}
		return body.documents();
	}

	/**
	 * 지번 주소에서 행정구역만 남긴다. "경기 가평군 북면 화악리 산 228-5"는 경기, 가평군, 북면, 화악리가 된다.
	 */
	static List<String> regionsOf(String addressName) {
		List<String> regions = new ArrayList<>();
		if (isBlank(addressName)) {
			return regions;
		}
		for (String token : addressName.strip().split("\\s+")) {
			if (token.equals("산") || Character.isDigit(token.charAt(0))) {
				break;
			}
			regions.add(token);
		}
		return List.copyOf(regions);
	}

	static GeocodePlace.Kind kindOf(String categoryName) {
		if (categoryName != null && DESTINATION_CATEGORIES.stream().anyMatch(categoryName::startsWith)
			&& !categoryName.startsWith("여행 > 숙박")) {
			return GeocodePlace.Kind.PLACE;
		}
		return GeocodePlace.Kind.OTHER;
	}

	private static boolean hasPosition(KakaoLocalResponse.Document document) {
		try {
			return !isBlank(document.x()) && !isBlank(document.y())
				&& Double.isFinite(Double.parseDouble(document.x())) && Double.isFinite(Double.parseDouble(document.y()));
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/** 카카오는 좌표를 소수 열네 자리까지 준다. 일곱 자리(1cm쯤)로 줄인다 */
	private static double coordinate(String value) {
		return Math.round(Double.parseDouble(value) * 1e7) / 1e7;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
