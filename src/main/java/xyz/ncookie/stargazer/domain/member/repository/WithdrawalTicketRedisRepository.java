package xyz.ncookie.stargazer.domain.member.repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 소셜 회원의 탈퇴 표. 로그인한 앱이 받아 제공자 로그인 주소에 실어 보내고, 콜백에서 한 번만 쓴다.
 * 표 없이 제공자 로그인만으로는 탈퇴되지 않으므로, 다른 사이트가 탈퇴 주소로 이끌어도 계정이 지워지지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class WithdrawalTicketRedisRepository {

	private static final String PREFIX = "withdrawal-ticket:";

	private final StringRedisTemplate redis;

	public void save(String ticket, Long memberId, Duration ttl) {
		redis.opsForValue().set(PREFIX + ticket, String.valueOf(memberId), ttl);
	}

	/** 표의 회원 ID를 돌려주고 표를 지운다. 없거나 시간이 지났으면 null */
	public Long consume(String ticket) {
		String value = redis.opsForValue().getAndDelete(PREFIX + ticket);
		return value == null ? null : Long.valueOf(value);
	}
}
