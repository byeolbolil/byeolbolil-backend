package xyz.ncookie.stargazer.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.bucket4j.Bucket;

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

	@Autowired
	protected MockMvcTester mvc;

	@Autowired
	protected ExternalApiStubs externalApi;

	@Autowired
	protected StringRedisTemplate redis;

	@Autowired
	protected TestClock clock;

	@Autowired
	private Bucket photonRateLimitBucket;

	@Autowired
	private Bucket kakaoRateLimitBucket;

	/**
	 * 컨텍스트를 공유하므로 테스트 사이에 남는 상태를 매번 비운다.
	 * Redis의 모든 키(예보, 지명 검색 캐시와 로그인 토큰), Photon, 카카오 호출 제한 토큰, 외부 API 대체 응답, 고정해 둔 시각이 대상이다.
	 */
	@BeforeEach
	void resetSharedState() {
		redis.execute((RedisCallback<Void>) connection -> {
			connection.serverCommands().flushDb();
			return null;
		});
		photonRateLimitBucket.reset();
		kakaoRateLimitBucket.reset();
		externalApi.reset();
		clock.reset();
	}
}
