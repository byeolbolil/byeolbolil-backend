package xyz.ncookie.stargazer.domain.geocode.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.geocode.cache.GeocodeCache;
import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.client.kakao.KakaoLocalClient;
import xyz.ncookie.stargazer.domain.geocode.client.photon.PhotonClient;
import xyz.ncookie.stargazer.domain.geocode.exception.GeocodeErrorCode;
import xyz.ncookie.stargazer.domain.geocode.exception.GeocodeException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;

/**
 * 지명으로 좌표를 찾고, 좌표가 어디쯤인지 이름을 붙인다.
 *
 * 이름 검색은 두 곳에 묻는다. OpenStreetMap(Photon)은 시와 군, 산 같은 지역을 잘 알고, 카카오 로컬은 전망대와 주차장
 * 같은 국내 장소와 지번 주소를 잘 안다. 섞는 규칙과 그 근거는 docs/decisions.md의 "지명 검색에 카카오 로컬을 더한다"에 있다.
 *
 * Photon의 공개 서버는 "공정하게 쓰라"는 조건만 걸고 한도를 숫자로 밝히지 않는다(https://photon.komoot.io/).
 * 그래서 같은 요청은 캐시로 막고, 밖으로 나가는 호출 수에도 상한을 둔다. 값을 고른 이유는 docs/decisions.md의
 * "지명 검색: Photon을 서버가 대신 부른다"에 있다. 카카오의 결과는 저장해 두지 않는다.
 */
@Slf4j
@Service
public class GeocodeDomainService {

	/** 한 번에 돌려주는 결과 수 */
	static final int MAX_RESULTS = 8;

	// 카카오의 결과가 있을 때 OpenStreetMap의 지역과 산이 앞에서 차지하는 자리 수. 나머지는 카카오 뒤로 간다.
	// 지역 이름으로 출발지를 찾는 사람과 전망대 이름으로 목적지를 찾는 사람이 모두 첫 화면에서 답을 보게 하려는 값이다
	static final int LEADING_AREAS = 3;

	// 주소 검색 결과가 차지하는 자리 수. 동 이름 하나에 "1가, 2가"가 줄줄이 오는 일이 있다
	static final int MAX_ADDRESSES = 2;

	// 지명과 행정구역은 자주 바뀌지 않는다. 하루로 두면 OpenStreetMap에서 고쳐진 내용도 다음 날에는 보인다.
	private static final Duration CACHE_TTL = Duration.ofHours(24);

	// 같은 이름이 이 거리 안에 또 있으면 같은 곳으로 본다. 한 역의 승강장과 출입구, 행정동과 법정동이 여기에 든다.
	private static final double SAME_PLACE_KM = 1.0;

	private static final double KM_PER_LATITUDE_DEGREE = 111.0;

	private final PhotonClient photonClient;
	private final KakaoLocalClient kakaoClient;
	private final GeocodeCache cache;
	private final Bucket photonLimit;
	private final Bucket kakaoLimit;

	public GeocodeDomainService(
		PhotonClient photonClient, KakaoLocalClient kakaoClient, GeocodeCache cache,
		Bucket photonRateLimitBucket, Bucket kakaoRateLimitBucket
	) {
		this.photonClient = photonClient;
		this.kakaoClient = kakaoClient;
		this.cache = cache;
		this.photonLimit = photonRateLimitBucket;
		this.kakaoLimit = kakaoRateLimitBucket;
	}

	/**
	 * 이름으로 장소를 찾는다. 두 소스 가운데 하나만 답해도 그 결과를 돌려주고, 둘 다 답하지 못할 때만 실패한다.
	 */
	public List<GeocodePlace> search(String rawQuery) {
		String query = normalize(rawQuery);

		Found kakao = searchKakao(query);
		List<GeocodePlace> osm;
		try {
			osm = searchOpenStreetMap(query);
		} catch (GeocodeException e) {
			if (!kakao.answered()) {
				throw e;
			}
			osm = List.of();
		}
		return arrange(osm, kakao.addresses(), kakao.places());
	}

	/** Photon이 돌려준 그대로를 기억해 둔다. 늘어놓기는 카카오의 결과와 섞은 뒤에 한다 */
	private List<GeocodePlace> searchOpenStreetMap(String query) {
		Optional<List<GeocodePlace>> cached = cache.findSearch(query);
		if (cached.isPresent()) {
			return cached.get();
		}
		List<GeocodePlace> places = callPhoton(() -> photonClient.search(query));
		cache.putSearch(query, places, CACHE_TTL);
		return places;
	}

	/**
	 * 카카오에 묻는다. 키가 없거나, 상한에 닿았거나, 카카오가 답하지 않으면 답하지 못한 것으로 돌려주고 검색은 이어 간다.
	 */
	private Found searchKakao(String query) {
		if (!kakaoClient.isConfigured()) {
			return Found.NONE;
		}
		if (!kakaoLimit.tryConsume(1)) {
			log.warn("카카오 로컬 호출이 상한에 닿아 건너뜁니다.");
			return Found.NONE;
		}
		try {
			return new Found(true, kakaoClient.searchAddresses(query), kakaoClient.searchPlaces(query));
		} catch (GeocodeProviderException e) {
			log.warn("카카오 로컬이 답하지 않았습니다: {}", e.getMessage());
			return Found.NONE;
		}
	}

	/**
	 * 좌표가 어디쯤인지 행정구역 이름으로 답한다. 근처에 아무것도 없으면 비어 있다.
	 */
	public Optional<String> findAreaName(double latitude, double longitude) {
		// 소수 셋째 자리는 100m쯤이다. 동네 이름을 붙이는 데는 그보다 정밀할 필요가 없다.
		double cellLatitude = roundToCell(latitude);
		double cellLongitude = roundToCell(longitude);
		String cell = String.format(Locale.ROOT, "%.3f,%.3f", cellLatitude, cellLongitude);

		Optional<List<GeocodePlace>> cached = cache.findReverse(cell);
		if (cached.isPresent()) {
			return cached.get().stream().findFirst().map(GeocodePlace::areaName);
		}

		Optional<GeocodePlace> found = callPhoton(() -> photonClient.reverse(cellLatitude, cellLongitude));
		cache.putReverse(cell, found.map(List::of).orElseGet(List::of), CACHE_TTL);
		return found.map(GeocodePlace::areaName);
	}

	private <T> T callPhoton(Supplier<T> call) {
		if (!photonLimit.tryConsume(1)) {
			throw new GeocodeException(GeocodeErrorCode.GEOCODE_BUSY);
		}
		try {
			return call.get();
		} catch (GeocodeProviderException e) {
			log.warn("지명 검색 소스가 답하지 않았습니다: {}", e.getMessage());
			throw new GeocodeException(GeocodeErrorCode.GEOCODE_UNAVAILABLE);
		}
	}

	/** 앞뒤 공백, 띄어쓰기 개수, 대소문자만 다른 검색어를 같은 캐시 항목으로 모은다. */
	static String normalize(String query) {
		return query.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	/** OpenStreetMap의 결과만 있을 때의 순서. 지역과 산을 앞에, 역과 도로 같은 나머지를 뒤에 둔다 */
	static List<GeocodePlace> arrange(List<GeocodePlace> osm) {
		return arrange(osm, List.of(), List.of());
	}

	/**
	 * 두 소스의 결과를 한 줄로 세우고, 겹치는 것을 지운 뒤 앞에서부터 자른다. 소스마다 받은 순서는 지킨다.
	 *
	 * 순서는 OpenStreetMap의 지역과 산 몇 곳, 카카오가 찾은 주소, 카카오의 목적지(관광지, 공원, 역, 주차장),
	 * OpenStreetMap의 남은 지역과 산, 카카오의 나머지(숙소, 가게), OpenStreetMap의 나머지(역, 도로)다.
	 * Photon은 이름이 똑같은 역과 정류장을 군이나 읍보다 앞에 주는데, 출발지나 관측지를 찾는 사람에게는 반대가 맞다.
	 * 카카오의 결과가 없으면 OpenStreetMap의 지역과 산이 자리 제한 없이 앞에 온다.
	 */
	static List<GeocodePlace> arrange(List<GeocodePlace> osm, List<GeocodePlace> addresses, List<GeocodePlace> kakao) {
		List<GeocodePlace> areas = ofKind(osm, GeocodePlace.Kind.PLACE);
		boolean mixed = !addresses.isEmpty() || !kakao.isEmpty();
		int leading = mixed ? Math.min(LEADING_AREAS, areas.size()) : areas.size();

		List<GeocodePlace> ordered = new ArrayList<>();
		ordered.addAll(areas.subList(0, leading));
		ordered.addAll(addresses.subList(0, Math.min(MAX_ADDRESSES, addresses.size())));
		ordered.addAll(ofKind(kakao, GeocodePlace.Kind.PLACE));
		ordered.addAll(areas.subList(leading, areas.size()));
		ordered.addAll(ofKind(kakao, GeocodePlace.Kind.OTHER));
		ordered.addAll(ofKind(osm, GeocodePlace.Kind.OTHER));

		List<GeocodePlace> kept = new ArrayList<>();
		for (GeocodePlace place : ordered) {
			if (kept.size() == MAX_RESULTS) {
				break;
			}
			if (kept.stream().noneMatch(other -> isSamePlace(other, place))) {
				kept.add(place);
			}
		}
		return List.copyOf(kept);
	}

	private static List<GeocodePlace> ofKind(List<GeocodePlace> places, GeocodePlace.Kind kind) {
		return places.stream().filter(place -> place.kind() == kind).toList();
	}

	private static boolean isSamePlace(GeocodePlace a, GeocodePlace b) {
		if (!withoutSpaces(a.name()).equals(withoutSpaces(b.name()))) {
			return false;
		}
		double northKm = (a.latitude() - b.latitude()) * KM_PER_LATITUDE_DEGREE;
		double eastKm = (a.longitude() - b.longitude()) * KM_PER_LATITUDE_DEGREE
			* Math.cos(Math.toRadians(a.latitude()));
		return Math.hypot(northKm, eastKm) <= SAME_PLACE_KM;
	}

	private static String withoutSpaces(String name) {
		return name.replace(" ", "");
	}

	private static double roundToCell(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	/**
	 * 카카오에 물은 결과.
	 *
	 * @param answered  카카오가 답했는지. 찾은 곳이 없다는 답도 답이다
	 * @param addresses 주소 검색으로 찾은 주소와 행정구역
	 * @param places    키워드 검색으로 찾은 장소
	 */
	private record Found(boolean answered, List<GeocodePlace> addresses, List<GeocodePlace> places) {
		static final Found NONE = new Found(false, List.of(), List.of());
	}
}
