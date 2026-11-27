package xyz.ncookie.stargazer.domain.member.repository;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 가입 전 이메일 인증 코드. 이메일마다 코드와 틀린 횟수, 다시 받기까지의 대기, 하루에 받은 횟수를 둔다.
 * 키의 이메일은 앞뒤 공백을 떼고 소문자로 바꾼 것이다.
 */
@Repository
@RequiredArgsConstructor
public class EmailCodeRedisRepository {

	private static final String CODE = "email-code:";
	private static final String FAILURES = "email-code-failures:";
	private static final String COOLDOWN = "email-code-cooldown:";
	private static final String DAILY = "email-code-daily:";
	private static final String SENT_TODAY = "email-code-sent:";

	private final StringRedisTemplate redis;

	public void saveCode(String email, String code, Duration ttl) {
		redis.opsForValue().set(CODE + email, code, ttl);
		redis.delete(FAILURES + email);
	}

	public String findCode(String email) {
		return redis.opsForValue().get(CODE + email);
	}

	public void deleteCode(String email) {
		redis.delete(CODE + email);
		redis.delete(FAILURES + email);
	}

	/** 틀린 횟수를 하나 올리고 올린 값을 돌려준다 */
	public long addFailure(String email, Duration ttl) {
		Long failures = redis.opsForValue().increment(FAILURES + email);
		redis.expire(FAILURES + email, ttl);
		return failures == null ? 0 : failures;
	}

	/** 대기 중이 아니면 대기를 걸고 true, 대기 중이면 false */
	public boolean startCooldown(String email, Duration cooldown) {
		return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(COOLDOWN + email, "1", cooldown));
	}

	public void clearCooldown(String email) {
		redis.delete(COOLDOWN + email);
	}

	/** 그날 이 이메일로 보낸 횟수를 하나 올리고 올린 값을 돌려준다 */
	public long countDaily(String email, LocalDate day) {
		return increment(DAILY + day + ":" + email);
	}

	/** 그날 서비스 전체가 보낸 메일 수를 하나 올리고 올린 값을 돌려준다 */
	public long countSentToday(LocalDate day) {
		return increment(SENT_TODAY + day);
	}

	private long increment(String key) {
		Long count = redis.opsForValue().increment(key);
		redis.expire(key, Duration.ofDays(2));
		return count == null ? 0 : count;
	}
}
