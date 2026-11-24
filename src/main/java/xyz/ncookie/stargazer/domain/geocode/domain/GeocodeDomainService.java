package xyz.ncookie.stargazer.domain.geocode.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.geocode.cache.GeocodeCache;
import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.client.photon.PhotonClient;
import xyz.ncookie.stargazer.domain.geocode.exception.GeocodeErrorCode;
import xyz.ncookie.stargazer.domain.geocode.exception.GeocodeException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;

/**
 * 지명으로 좌표를 찾고, 좌표가 어디쯤인지 이름을 붙인다.
 *
 * Photon의 공개 서버는 "공정하게 쓰라"는 조건만 걸고 한도를 숫자로 밝히지 않는다(https://photon.komoot.io/).
 * 그래서 같은 요청은 캐시로 막고, 밖으로 나가는 호출 수에도 상한을 둔다. 값을 고른 이유는 docs/decisions.md의
 * "지명 검색: Photon을 서버가 대신 부른다"에 있다.
 */
@Slf4j
@Service
public class GeocodeDomainService {

	/** 한 번에 돌려주는 결과 수 */
	static final int MAX_RESULTS = 8;

	// 지명과 행정구역은 자주 바뀌지 않는다. 하루로 두면 OpenStreetMap에서 고쳐진 내용도 다음 날에는 보인다.
	private static final Duration CACHE_TTL = Duration.ofHours(24);

	// 같은 이름이 이 거리 안에 또 있으면 같은 곳으로 본다. 한 역의 승강장과 출입구, 행정동과 법정동이 여기에 든다.
	private static final double SAME_PLACE_KM = 1.0;

	private static final double KM_PER_LATITUDE_DEGREE = 111.0;

	private final PhotonClient photonClient;
	private final GeocodeCache cache;
	private final Bucket outboundLimit;

	public GeocodeDomainService(PhotonClient photonClient, GeocodeCache cache, Bucket photonRateLimitBucket) {
		this.photonClient = photonClient;
		this.cache = cache;
		this.outboundLimit = photonRateLimitBucket;
	}

	/**
	 * 이름으로 장소를 찾는다. 지역과 산, 천문대 같은 곳을 앞에 두고 역과 도로 같은 나머지를 뒤에 둔다.
	 * Photon은 이름이 똑같은 역과 정류장을 군이나 읍보다 앞에 주는데, 출발지나 관측지를 찾는 사람에게는 반대가 맞다.
	 */
	public List<GeocodePlace> search(String rawQuery) {
		String query = normalize(rawQuery);

		Optional<List<GeocodePlace>> cached = cache.findSearch(query);
		if (cached.isPresent()) {
			return cached.get();
		}

		List<GeocodePlace> places = arrange(callPhoton(() -> photonClient.search(query)));
		cache.putSearch(query, places, CACHE_TTL);
		return places;
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
		if (!outboundLimit.tryConsume(1)) {
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

	/**
	 * 종류 순으로 늘어놓고(같은 종류 안에서는 Photon의 순서를 지킨다) 겹치는 것을 지운 뒤 앞에서부터 자른다.
	 */
	static List<GeocodePlace> arrange(List<GeocodePlace> found) {
		List<GeocodePlace> sorted = new ArrayList<>(found);
		sorted.sort(Comparator.comparing(GeocodePlace::kind));

		List<GeocodePlace> kept = new ArrayList<>();
		for (GeocodePlace place : sorted) {
			if (kept.size() == MAX_RESULTS) {
				break;
			}
			if (kept.stream().noneMatch(other -> isSamePlace(other, place))) {
				kept.add(place);
			}
		}
		return List.copyOf(kept);
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
}
