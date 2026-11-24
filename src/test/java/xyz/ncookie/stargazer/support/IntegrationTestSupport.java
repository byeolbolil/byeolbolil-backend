package xyz.ncookie.stargazer.support;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.bucket4j.Bucket;
import xyz.ncookie.stargazer.infra.lightpollution.LightPollutionMigrationRunner;

/**
 * 통합 테스트의 공통 기반.
 *
 * 애플리케이션 전체를 띄우되 MySQL과 Redis는 Testcontainers로, 외부 API는 ExternalApiStubs로 바꾼다.
 * 이 클래스를 상속한 테스트는 같은 컨텍스트 설정을 쓰므로 컨텍스트와 컨테이너를 공유한다.
 * 설정이 다른 컨텍스트가 필요해지면 컨테이너가 한 벌 더 뜬다는 점을 감안해야 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestContainersConfig.class, ExternalApiStubConfig.class, TestClockConfig.class})
public abstract class IntegrationTestSupport {

	/**
	 * 기동할 때 광해 CSV 58만 행을 적재하는 러너를 끈다.
	 * 광해 데이터가 필요한 테스트는 필요한 격자점만 직접 넣는다.
	 */
	@MockitoBean
	private LightPollutionMigrationRunner lightPollutionMigrationRunner;

	@Autowired
	protected MockMvcTester mvc;

	@Autowired
	protected ExternalApiStubs externalApi;

	@Autowired
	protected StringRedisTemplate redis;

	@Autowired
	protected TestClock clock;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private Bucket openWeatherRateLimitBucket;

	@Autowired
	private Bucket photonRateLimitBucket;

	/**
	 * 컨텍스트를 공유하므로 테스트 사이에 남는 상태를 매번 비운다.
	 * 캐시(Caffeine, Redis), Redis의 나머지 키, OpenWeather와 Photon 호출 제한 토큰, 외부 API 대체 응답, 고정해 둔 시각이 대상이다.
	 */
	@BeforeEach
	void resetSharedState() {
		clearCaches();
		redis.execute((RedisCallback<Void>) connection -> {
			connection.serverCommands().flushDb();
			return null;
		});
		openWeatherRateLimitBucket.reset();
		photonRateLimitBucket.reset();
		externalApi.reset();
		clock.reset();
	}

	/**
	 * Spring Data Redis 4는 캐시 쓰기와 지우기를 비동기로 보낸다. 지우기가 끝나기 전에 다음 테스트가 시작되면
	 * 뒤늦게 도착한 삭제가 새 테스트의 캐시 항목을 지울 수 있어서, Redis에서 캐시 키가 사라질 때까지 기다린다.
	 */
	private void clearCaches() {
		Collection<String> cacheNames = cacheManager.getCacheNames();
		for (String name : cacheNames) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				cache.clear();
			}
		}
		await().atMost(Duration.ofSeconds(5)).until(() -> cacheNames.stream().allMatch(this::hasNoCachedKeys));
	}

	private boolean hasNoCachedKeys(String cacheName) {
		Set<String> keys = redis.keys(cacheName + "::*");
		return keys == null || keys.isEmpty();
	}
}
