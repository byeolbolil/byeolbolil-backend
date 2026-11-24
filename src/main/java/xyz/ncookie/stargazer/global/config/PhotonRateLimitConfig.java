package xyz.ncookie.stargazer.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Photon 지명 검색으로 나가는 호출의 상한. 캐시에 없어서 실제로 호출할 때만 토큰을 쓴다.
 *
 * Photon은 한도를 숫자로 밝히지 않아 근거로 삼을 값이 없다. 우리 API가 남용돼도 Photon에는 초당 5회를 넘겨 보내지
 * 않도록 잡은 운영상의 값이다(docs/decisions.md "지명 검색: Photon을 서버가 대신 부른다").
 */
@Configuration
public class PhotonRateLimitConfig {

	public static final int BURST = 10;
	public static final int PER_SECOND = 5;

	@Bean
	public Bucket photonRateLimitBucket() {
		Bandwidth limit = Bandwidth.builder()
			.capacity(BURST)
			.refillGreedy(PER_SECOND, Duration.ofSeconds(1))
			.build();
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}
}
