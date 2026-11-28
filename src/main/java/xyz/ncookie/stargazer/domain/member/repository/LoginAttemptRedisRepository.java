package xyz.ncookie.stargazer.domain.member.repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 이메일 로그인에서 틀린 횟수. 키의 이메일은 앞뒤 공백을 떼고 소문자로 바꾼 것이다.
 */
@Repository
@RequiredArgsConstructor
public class LoginAttemptRedisRepository {

	private static final String PREFIX = "login-failures:";

	private final StringRedisTemplate redis;

	public long failures(String email) {
		String value = redis.opsForValue().get(PREFIX + email);
		return value == null ? 0 : Long.parseLong(value);
	}

	/** 하나 올리고 올린 값을 돌려준다. 처음 틀린 때부터 {@code window}가 지나면 저절로 지워진다 */
	public long addFailure(String email, Duration window) {
		Long count = redis.opsForValue().increment(PREFIX + email);
		if (count != null && count == 1) {
			redis.expire(PREFIX + email, window);
		}
		return count == null ? 0 : count;
	}

	/** 지금부터 {@code duration} 동안 횟수를 남겨 둔다. 막기 시작할 때 막는 시간을 그때부터 센다 */
	public void hold(String email, Duration duration) {
		redis.expire(PREFIX + email, duration);
	}

	public void clear(String email) {
		redis.delete(PREFIX + email);
	}
}
