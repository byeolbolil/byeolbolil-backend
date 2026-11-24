package xyz.ncookie.stargazer.domain.weather.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.weather.cache.ForecastCache;
import xyz.ncookie.stargazer.domain.weather.client.WeatherProviderException;
import xyz.ncookie.stargazer.domain.weather.client.metnorway.MetNorwayClient;
import xyz.ncookie.stargazer.domain.weather.client.metnorway.MetNorwayForecast;
import xyz.ncookie.stargazer.domain.weather.client.openmeteo.OpenMeteoClient;
import xyz.ncookie.stargazer.domain.weather.exception.WeatherErrorCode;
import xyz.ncookie.stargazer.domain.weather.exception.WeatherException;
import xyz.ncookie.stargazer.domain.weather.model.CloudGridForecast;
import xyz.ncookie.stargazer.domain.weather.model.CloudGridSpec;
import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;
import xyz.ncookie.stargazer.domain.weather.model.ModelRun;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * 좌표의 시간별 예보를 내부 모델로 돌려준다.
 *
 * 주 소스는 Open-Meteo, 보조 소스는 MET Norway다. 예보는 좌표가 아니라 격자 칸(ForecastCell)과
 * 모델 실행 시각으로 캐시한다. 같은 실행의 같은 칸은 누가 물어도 같은 예보이기 때문이다.
 * 여기 있는 시간 값들은 운영 정책이고, 고른 이유는 docs/decisions.md에 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDomainService {

	/** 돌려주는 예보의 길이. 7일 */
	public static final int FORECAST_HOURS = 168;

	// 모델 실행 시각을 다시 확인하는 주기. 새 실행이 나와도 이 시간만큼은 이전 실행의 예보를 쓴다.
	static final Duration MODEL_RUN_CHECK_INTERVAL = Duration.ofMinutes(10);

	// 모델 실행 시각을 한 번도 확인하지 못한 채 받은 예보를 두는 시간. 어느 실행인지 모르니 짧게 둔다.
	static final Duration UNKNOWN_RUN_TTL = Duration.ofHours(1);

	// 주 소스가 실패한 뒤 다시 시도하지 않고 바로 보조 소스로 가는 시간.
	// 장소 수십 곳을 한꺼번에 계산할 때 곳마다 시간 초과를 기다리지 않게 한다.
	static final Duration PRIMARY_RETRY_DELAY = Duration.ofMinutes(1);

	// MET Norway 응답을 두는 시간의 범위와, Expires 헤더가 없을 때의 값.
	static final Duration FALLBACK_TTL_MIN = Duration.ofMinutes(1);
	static final Duration FALLBACK_TTL_MAX = Duration.ofHours(1);
	static final Duration FALLBACK_TTL_DEFAULT = Duration.ofMinutes(30);

	private final OpenMeteoClient openMeteoClient;
	private final MetNorwayClient metNorwayClient;
	private final ForecastCache forecastCache;
	private final Clock clock;

	private final Object cloudGridLock = new Object();

	/**
	 * 지금 시각이 속한 정시부터 최대 7일의 시간별 예보.
	 * 보조 소스로 받은 경우에는 이틀 반 정도만 들어 있고 시정이 없다.
	 *
	 * @throws WeatherException 두 소스 모두 답하지 못했을 때
	 */
	public WeatherForecast getHourlyForecast(double latitude, double longitude) {
		ForecastCell cell = ForecastCell.of(latitude, longitude);
		Instant now = clock.instant();

		WeatherForecast forecast = fromPrimary(cell, now)
			.or(() -> fromFallback(cell, now))
			.orElseThrow(() -> new WeatherException(WeatherErrorCode.FORECAST_UNAVAILABLE));

		WeatherForecast window = forecast.startingAt(now.truncatedTo(ChronoUnit.HOURS), FORECAST_HOURS);
		if (window.hours().isEmpty()) {
			throw new WeatherException(WeatherErrorCode.FORECAST_UNAVAILABLE);
		}
		return window;
	}

	/**
	 * 한국을 덮는 격자의 시간별 총운량. 지도에 구름을 그리는 데 쓴다.
	 *
	 * 모델 실행마다 한 번만 받아 캐시에 두고, 실행이 바뀌기 전까지는 누가 물어도 그것을 준다.
	 * 여러 지점을 한 번에 받는 API는 Open-Meteo에만 있어서 보조 소스가 없다. 받지 못하면 잠시 동안은 다시 시도하지 않는다.
	 *
	 * @throws WeatherException 격자 예보를 받지 못했을 때
	 */
	public CloudGridForecast getCloudGrid() {
		Instant now = clock.instant();
		boolean down = forecastCache.isCloudGridDown();
		Optional<ModelRun> run = down ? forecastCache.findModelRun(openMeteoClient.model()) : currentModelRun();
		String key = cloudGridKey(run, now);

		Optional<CloudGridForecast> cached = forecastCache.findCloudGrid(key);
		if (cached.isPresent()) {
			return cached.get();
		}
		if (down) {
			throw new WeatherException(WeatherErrorCode.FORECAST_UNAVAILABLE);
		}

		// 요청이 몰려도 306점짜리 호출은 한 번만 나가게 한다. 기다린 요청은 앞선 요청이 넣어 둔 값을 읽는다.
		synchronized (cloudGridLock) {
			Optional<CloudGridForecast> filled = forecastCache.findCloudGrid(key);
			if (filled.isPresent()) {
				return filled.get();
			}
			if (forecastCache.isCloudGridDown()) {
				throw new WeatherException(WeatherErrorCode.FORECAST_UNAVAILABLE);
			}
			try {
				CloudGridForecast fetched = openMeteoClient.fetchCloudGrid(
					CloudGridSpec.KOREA, run.map(ModelRun::initialisedAt).orElse(null));
				forecastCache.putCloudGrid(key, fetched, primaryTtl(run));
				return fetched;
			} catch (WeatherProviderException e) {
				log.warn("Open-Meteo 격자 예보를 받지 못했습니다.", e);
				forecastCache.markCloudGridDown(PRIMARY_RETRY_DELAY);
				throw new WeatherException(WeatherErrorCode.FORECAST_UNAVAILABLE);
			}
		}
	}

	private String cloudGridKey(Optional<ModelRun> run, Instant now) {
		String runPart = run
			.map(r -> String.valueOf(r.initialisedAt().getEpochSecond()))
			.orElseGet(() -> "unknown-" + now.truncatedTo(ChronoUnit.HOURS).getEpochSecond());
		return openMeteoClient.model() + ":" + runPart;
	}

	/**
	 * 주 소스의 예보. 캐시에 있으면 외부로 나가지 않는다.
	 * 주 소스가 방금 실패했더라도, 마지막으로 확인한 실행의 예보가 캐시에 남아 있으면 그것을 쓴다.
	 */
	private Optional<WeatherForecast> fromPrimary(ForecastCell cell, Instant now) {
		boolean primaryDown = forecastCache.isPrimaryDown();
		Optional<ModelRun> run = primaryDown
			? forecastCache.findModelRun(openMeteoClient.model())
			: currentModelRun();

		String key = primaryKey(cell, run, now);
		Optional<WeatherForecast> cached = forecastCache.findForecast(key);
		if (cached.isPresent() || primaryDown) {
			return cached;
		}

		try {
			Instant issuedAt = run.map(ModelRun::initialisedAt).orElse(null);
			WeatherForecast fetched = openMeteoClient.fetchForecast(cell, issuedAt);
			forecastCache.putForecast(key, fetched, primaryTtl(run));
			return Optional.of(fetched);
		} catch (WeatherProviderException e) {
			log.warn("Open-Meteo 예보를 받지 못해 보조 소스로 넘어갑니다. cell={}", cell.key(), e);
			forecastCache.markPrimaryDown(PRIMARY_RETRY_DELAY);
			return Optional.empty();
		}
	}

	/**
	 * 지금 내주는 모델 실행. 최근에 확인했으면 기억해 둔 값을 쓰고, 아니면 다시 조회한다.
	 * 조회에 실패하면 마지막으로 확인한 값을 쓴다. 예보 자체는 받을 수 있을지 모르므로 여기서 주 소스를 포기하지 않는다.
	 */
	private Optional<ModelRun> currentModelRun() {
		String model = openMeteoClient.model();
		Optional<ModelRun> remembered = forecastCache.findModelRun(model);
		if (remembered.isPresent() && forecastCache.isModelRunFresh(model)) {
			return remembered;
		}

		try {
			ModelRun run = openMeteoClient.fetchLatestRun();
			forecastCache.putModelRun(model, run, MODEL_RUN_CHECK_INTERVAL, run.updateInterval().multipliedBy(2));
			return Optional.of(run);
		} catch (WeatherProviderException e) {
			log.warn("Open-Meteo 모델 실행 시각을 확인하지 못했습니다. model={}", model, e);
			return remembered;
		}
	}

	private String primaryKey(ForecastCell cell, Optional<ModelRun> run, Instant now) {
		String runPart = run
			.map(r -> String.valueOf(r.initialisedAt().getEpochSecond()))
			.orElseGet(() -> "unknown-" + now.truncatedTo(ChronoUnit.HOURS).getEpochSecond());
		return openMeteoClient.model() + ":" + runPart + ":" + cell.key();
	}

	/**
	 * 다음 실행이 나오면 키가 바뀌어 이 항목은 더 읽히지 않는다. 만료 시간은 Redis에서 치우기 위한 것이다.
	 * 실행이 늦게 올라오는 경우를 감안해 갱신 주기의 두 배로 둔다.
	 */
	private Duration primaryTtl(Optional<ModelRun> run) {
		return run.map(r -> r.updateInterval().multipliedBy(2)).orElse(UNKNOWN_RUN_TTL);
	}

	private Optional<WeatherForecast> fromFallback(ForecastCell cell, Instant now) {
		String key = "met-norway:" + cell.key();
		Optional<WeatherForecast> cached = forecastCache.findForecast(key);
		if (cached.isPresent()) {
			return cached;
		}

		try {
			MetNorwayForecast fetched = metNorwayClient.fetchForecast(cell);
			forecastCache.putForecast(key, fetched.forecast(), fallbackTtl(fetched.expiresAt(), now));
			return Optional.of(fetched.forecast());
		} catch (WeatherProviderException e) {
			log.error("MET Norway 예보도 받지 못했습니다. cell={}", cell.key(), e);
			return Optional.empty();
		}
	}

	/**
	 * MET Norway는 Expires 전에 같은 자료를 다시 요청하지 말라고 요구한다. 그 시각까지 캐시에 둔다.
	 */
	private Duration fallbackTtl(Instant expiresAt, Instant now) {
		if (expiresAt == null) {
			return FALLBACK_TTL_DEFAULT;
		}
		Duration untilExpiry = Duration.between(now, expiresAt);
		if (untilExpiry.compareTo(FALLBACK_TTL_MIN) < 0) {
			return FALLBACK_TTL_MIN;
		}
		if (untilExpiry.compareTo(FALLBACK_TTL_MAX) > 0) {
			return FALLBACK_TTL_MAX;
		}
		return untilExpiry;
	}
}
