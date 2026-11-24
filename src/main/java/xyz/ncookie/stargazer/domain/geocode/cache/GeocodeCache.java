package xyz.ncookie.stargazer.domain.geocode.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;

/**
 * 지명 검색 결과를 Redis에 둔다. 같은 검색어와 같은 좌표로 Photon을 다시 부르지 않기 위해서다.
 *
 * Redis가 응답하지 않아도 검색 자체는 되어야 하므로, 읽기 실패는 "없음"으로, 쓰기 실패는 무시로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeocodeCache {

	private static final String PREFIX = "geocode:v1:";
	private static final TypeReference<List<GeocodePlace>> PLACES = new TypeReference<>() {};

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public Optional<List<GeocodePlace>> findSearch(String query) {
		return read(searchKey(query));
	}

	public void putSearch(String query, List<GeocodePlace> places, Duration ttl) {
		write(searchKey(query), places, ttl);
	}

	/**
	 * 좌표의 결과. 근처에 아무것도 없다는 답도 빈 목록으로 기억한다.
	 */
	public Optional<List<GeocodePlace>> findReverse(String cell) {
		return read(reverseKey(cell));
	}

	public void putReverse(String cell, List<GeocodePlace> places, Duration ttl) {
		write(reverseKey(cell), places, ttl);
	}

	private static String searchKey(String query) {
		return PREFIX + "search:" + query;
	}

	private static String reverseKey(String cell) {
		return PREFIX + "reverse:" + cell;
	}

	private Optional<List<GeocodePlace>> read(String key) {
		String json;
		try {
			json = redis.opsForValue().get(key);
		} catch (RuntimeException e) {
			log.warn("지명 검색 캐시를 읽지 못했습니다. key={}", key, e);
			return Optional.empty();
		}
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, PLACES));
		} catch (RuntimeException e) {
			log.warn("캐시에 있던 지명 검색 결과를 읽지 못해 버립니다. key={}", key, e);
			return Optional.empty();
		}
	}

	private void write(String key, List<GeocodePlace> places, Duration ttl) {
		try {
			redis.opsForValue().set(key, objectMapper.writeValueAsString(places), ttl);
		} catch (RuntimeException e) {
			log.warn("지명 검색 캐시에 쓰지 못했습니다. key={}", key, e);
		}
	}
}
