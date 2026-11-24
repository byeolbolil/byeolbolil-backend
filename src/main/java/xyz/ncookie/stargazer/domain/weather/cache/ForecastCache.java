package xyz.ncookie.stargazer.domain.weather.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.domain.weather.model.CloudGridForecast;
import xyz.ncookie.stargazer.domain.weather.model.ModelRun;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * 예보와, 예보를 받는 데 필요한 상태(모델 실행 시각, 주 소스 장애 표시)를 Redis에 둔다.
 *
 * 항목마다 만료 시간이 달라서 @Cacheable 대신 직접 읽고 쓴다.
 * 상태를 Redis에 두는 이유는 인스턴스가 여러 대여도 같은 판단을 하게 하기 위해서다.
 *
 * Redis가 응답하지 않아도 예보 조회 자체는 실패하지 않아야 하므로, 읽기 실패는 "없음"으로, 쓰기 실패는 무시로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForecastCache {

	private static final String PREFIX = "forecast:v1:";
	private static final String PRIMARY_DOWN_KEY = PREFIX + "primary-down";
	private static final String CLOUD_GRID_DOWN_KEY = PREFIX + "cloud-grid-down";

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public Optional<WeatherForecast> findForecast(String key) {
		return read(forecastKey(key)).flatMap(this::deserialize);
	}

	public void putForecast(String key, WeatherForecast forecast, Duration ttl) {
		String json;
		try {
			json = objectMapper.writeValueAsString(forecast);
		} catch (RuntimeException e) {
			log.warn("예보를 직렬화하지 못해 캐시에 넣지 않습니다. key={}", key, e);
			return;
		}
		write(forecastKey(key), json, ttl);
	}

	/**
	 * 마지막으로 확인한 모델 실행. 최근에 확인한 값인지는 isModelRunFresh로 따로 본다.
	 */
	public Optional<ModelRun> findModelRun(String model) {
		return read(runKey(model)).flatMap(ForecastCache::parseModelRun);
	}

	public boolean isModelRunFresh(String model) {
		return read(runFreshKey(model)).isPresent();
	}

	/**
	 * @param freshFor 이 시간 동안은 다시 확인하지 않는다
	 * @param keepFor  다시 확인하지 못해도 이 시간 동안은 이 값을 기억한다
	 */
	public void putModelRun(String model, ModelRun run, Duration freshFor, Duration keepFor) {
		String value = run.initialisedAt().getEpochSecond() + ":" + run.updateInterval().toSeconds();
		write(runKey(model), value, keepFor);
		write(runFreshKey(model), "1", freshFor);
	}

	/**
	 * 구름 지도용 격자 예보. 모델 실행마다 하나다.
	 */
	public Optional<CloudGridForecast> findCloudGrid(String key) {
		return read(cloudGridKey(key)).flatMap(json -> {
			try {
				return Optional.of(objectMapper.readValue(json, CloudGridForecast.class));
			} catch (RuntimeException e) {
				log.warn("캐시에 있던 격자 예보를 읽지 못해 버립니다.", e);
				return Optional.empty();
			}
		});
	}

	public void putCloudGrid(String key, CloudGridForecast grid, Duration ttl) {
		String json;
		try {
			json = objectMapper.writeValueAsString(grid);
		} catch (RuntimeException e) {
			log.warn("격자 예보를 직렬화하지 못해 캐시에 넣지 않습니다. key={}", key, e);
			return;
		}
		write(cloudGridKey(key), json, ttl);
	}

	public boolean isCloudGridDown() {
		return read(CLOUD_GRID_DOWN_KEY).isPresent();
	}

	public void markCloudGridDown(Duration ttl) {
		write(CLOUD_GRID_DOWN_KEY, "1", ttl);
	}

	public boolean isPrimaryDown() {
		return read(PRIMARY_DOWN_KEY).isPresent();
	}

	public void markPrimaryDown(Duration ttl) {
		write(PRIMARY_DOWN_KEY, "1", ttl);
	}

	private static String cloudGridKey(String key) {
		return PREFIX + "cloud-grid:" + key;
	}

	private static String forecastKey(String key) {
		return PREFIX + "data:" + key;
	}

	private static String runKey(String model) {
		return PREFIX + "run:" + model;
	}

	private static String runFreshKey(String model) {
		return PREFIX + "run-fresh:" + model;
	}

	private static Optional<ModelRun> parseModelRun(String value) {
		String[] parts = value.split(":");
		if (parts.length != 2) {
			return Optional.empty();
		}
		try {
			return Optional.of(new ModelRun(
				Instant.ofEpochSecond(Long.parseLong(parts[0])),
				Duration.ofSeconds(Long.parseLong(parts[1]))
			));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private Optional<String> read(String key) {
		try {
			return Optional.ofNullable(redis.opsForValue().get(key));
		} catch (RuntimeException e) {
			log.warn("예보 캐시를 읽지 못했습니다. key={}", key, e);
			return Optional.empty();
		}
	}

	private void write(String key, String value, Duration ttl) {
		try {
			redis.opsForValue().set(key, value, ttl);
		} catch (RuntimeException e) {
			log.warn("예보 캐시에 쓰지 못했습니다. key={}", key, e);
		}
	}

	private Optional<WeatherForecast> deserialize(String json) {
		try {
			return Optional.of(objectMapper.readValue(json, WeatherForecast.class));
		} catch (RuntimeException e) {
			log.warn("캐시에 있던 예보를 읽지 못해 버립니다.", e);
			return Optional.empty();
		}
	}
}
