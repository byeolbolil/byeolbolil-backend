package xyz.ncookie.stargazer.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * 카카오 로컬 API로 나가는 검색 수의 상한. 토큰 하나가 검색 한 번이고, 검색 한 번은 키워드 검색과 주소 검색을
 * 한 번씩 부른다.
 *
 * 카카오의 무료 쿼터는 키워드 검색과 주소 검색이 각각 하루 100,000건이다
 * (https://developers.kakao.com/docs/latest/ko/getting-started/quota). 하루 상한은 그보다 조금 아래에 둔다.
 * 초당 상한은 Photon과 같은 값으로 두었다. 상한에 닿으면 카카오를 건너뛰고 OpenStreetMap 결과만 돌려준다.
 */
@Configuration
public class KakaoRateLimitConfig {

	public static final int BURST = 10;
	public static final int PER_SECOND = 5;
	public static final int PER_DAY = 90_000;

	@Bean
	public Bucket kakaoRateLimitBucket() {
		Bandwidth perSecond = Bandwidth.builder()
			.capacity(BURST)
			.refillGreedy(PER_SECOND, Duration.ofSeconds(1))
			.build();
		Bandwidth perDay = Bandwidth.builder()
			.capacity(PER_DAY)
			.refillIntervally(PER_DAY, Duration.ofDays(1))
			.build();
		return Bucket.builder()
			.addLimit(perSecond)
			.addLimit(perDay)
			.build();
	}
}
