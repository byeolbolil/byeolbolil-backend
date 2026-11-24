package xyz.ncookie.stargazer.domain.geocode.client.photon;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;

/**
 * Photon 지명 검색 API 클라이언트. OpenStreetMap 데이터를 쓰고 키가 필요 없다.
 * https://photon.komoot.io/ (요청과 응답 형식은 https://github.com/komoot/photon 의 README)
 *
 * 실패는 모두 GeocodeProviderException으로 던진다.
 */
@Component
public class PhotonClient {

	// 한국만 찾는다. 사각형에는 북한과 쓰시마가 걸치므로 응답의 국가 코드로 한 번 더 거른다.
	private static final String KOREA_BBOX = "124.5,33.0,131.9,38.7";
	private static final String KOREA = "KR";

	// 같은 이름의 역과 정류장이 앞쪽을 채우는 일이 많아서 넉넉히 받아 온 뒤 추린다.
	private static final int SEARCH_LIMIT = 30;

	// 산속에서는 가장 가까운 지물도 멀다. Photon의 기본 반경 1km로는 빈 응답이 잦다.
	private static final int REVERSE_RADIUS_KM = 10;

	// 지역은 Photon이 매긴 단계로, 목적지가 될 만한 곳은 OpenStreetMap 태그의 키로 알아본다.
	private static final Set<String> AREA_TYPES = Set.of("state", "county", "city", "district", "locality");
	private static final Set<String> PLACE_KEYS = Set.of(
		"place", "boundary", "natural", "tourism", "leisure", "historic", "mountain_pass");

	private final RestTemplate restTemplate;
	private final String baseUrl;

	public PhotonClient(RestTemplate restTemplate, @Value("${geocode.photon.base-url}") String baseUrl) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
	}

	/**
	 * 이름으로 장소를 찾는다. Photon이 돌려준 순서를 그대로 둔다.
	 */
	public List<GeocodePlace> search(String query) {
		URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/api/")
			.queryParam("q", "{q}")
			.queryParam("limit", SEARCH_LIMIT)
			.queryParam("lang", "default")
			.queryParam("bbox", KOREA_BBOX)
			.encode()
			.buildAndExpand(query)
			.toUri();

		return toPlaces(get(uri));
	}

	/**
	 * 좌표에서 가장 가까운 장소를 찾는다. 바다 한가운데처럼 근처에 아무것도 없으면 비어 있다.
	 */
	public Optional<GeocodePlace> reverse(double latitude, double longitude) {
		URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/reverse")
			.queryParam("lat", latitude)
			.queryParam("lon", longitude)
			.queryParam("limit", 1)
			.queryParam("lang", "default")
			.queryParam("radius", REVERSE_RADIUS_KM)
			.build()
			.toUri();

		return toPlaces(get(uri)).stream().findFirst();
	}

	private PhotonResponse get(URI uri) {
		PhotonResponse body;
		try {
			body = restTemplate.getForObject(uri, PhotonResponse.class);
		} catch (RestClientException e) {
			throw new GeocodeProviderException("Photon 호출 실패: " + e.getMessage(), e);
		}
		if (body == null || body.features() == null) {
			throw new GeocodeProviderException("Photon이 빈 응답을 돌려줬습니다.");
		}
		return body;
	}

	private static List<GeocodePlace> toPlaces(PhotonResponse response) {
		List<GeocodePlace> places = new ArrayList<>();
		for (PhotonResponse.Feature feature : response.features()) {
			PhotonResponse.Properties properties = feature.properties();
			PhotonResponse.Geometry geometry = feature.geometry();
			if (properties == null || geometry == null || geometry.coordinates() == null
				|| geometry.coordinates().size() < 2) {
				continue;
			}
			if (properties.name() == null || properties.name().isBlank() || !KOREA.equals(properties.countrycode())) {
				continue;
			}
			places.add(new GeocodePlace(
				properties.name().strip(),
				regionsOf(properties),
				kindOf(properties),
				geometry.coordinates().get(1),
				geometry.coordinates().get(0)
			));
		}
		return places;
	}

	private static List<String> regionsOf(PhotonResponse.Properties properties) {
		List<String> regions = new ArrayList<>();
		for (String region : new String[] {
			properties.state(), properties.county(), properties.city(), properties.district()}) {
			if (region != null && !region.isBlank() && !regions.contains(region)) {
				regions.add(region);
			}
		}
		return List.copyOf(regions);
	}

	private static GeocodePlace.Kind kindOf(PhotonResponse.Properties properties) {
		boolean area = properties.type() != null && AREA_TYPES.contains(properties.type());
		boolean destination = properties.osmKey() != null && PLACE_KEYS.contains(properties.osmKey());
		return area || destination ? GeocodePlace.Kind.PLACE : GeocodePlace.Kind.OTHER;
	}
}
