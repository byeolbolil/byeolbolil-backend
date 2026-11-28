package xyz.ncookie.stargazer.global.security.redis;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 리프레시 토큰. 토큰마다 회원 ID를 두고({@code RT:토큰}), 회원마다 가진 토큰 목록도 둔다({@code RT-MEMBER:회원 ID}).
 * 목록은 비밀번호를 바꾸거나 탈퇴할 때 그 회원의 모든 기기에서 로그인을 끊는 데 쓴다.
 * 목록이 생기기 전(2026-11-28)에 발급한 토큰은 목록에 없어서, 재발급할 때 회원이 없으면 거절하는 검사가 남은 몫을 막는다.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

	private final StringRedisTemplate redisTemplate;

	private static final String PREFIX = "RT:";
	private static final String MEMBER_PREFIX = "RT-MEMBER:";

	public void save(String refreshToken, Long memberId, long expireMs) {
		redisTemplate.opsForValue()
			.set(PREFIX + refreshToken, String.valueOf(memberId), expireMs, TimeUnit.MILLISECONDS);
		// 목록은 가장 늦게 만료되는 토큰만큼 산다. 만료된 토큰이 목록에 남아도 지울 때 없는 키를 지울 뿐이다
		redisTemplate.opsForSet().add(MEMBER_PREFIX + memberId, refreshToken);
		redisTemplate.expire(MEMBER_PREFIX + memberId, expireMs, TimeUnit.MILLISECONDS);
	}

	public Long find(String refreshToken) {
		String value = redisTemplate.opsForValue().get(PREFIX + refreshToken);

		if (value == null) {
			return null;
		}

		return Long.valueOf(value);
	}

	public void delete(String refreshToken) {
		String memberId = redisTemplate.opsForValue().getAndDelete(PREFIX + refreshToken);
		if (memberId != null) {
			redisTemplate.opsForSet().remove(MEMBER_PREFIX + memberId, refreshToken);
		}
	}

	/** 그 회원의 모든 기기에서 로그인을 끊는다 */
	public void deleteAllOf(Long memberId) {
		Set<String> tokens = redisTemplate.opsForSet().members(MEMBER_PREFIX + memberId);
		if (tokens != null && !tokens.isEmpty()) {
			redisTemplate.delete(tokens.stream().map(token -> PREFIX + token).toList());
		}
		redisTemplate.delete(MEMBER_PREFIX + memberId);
	}
}
