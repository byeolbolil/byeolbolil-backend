package xyz.ncookie.stargazer.domain.member.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.exception.EmailCodeErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.EmailCodeRedisRepository;

/**
 * 자체 가입 전에 그 이메일을 받을 수 있는 사람인지 여섯 자리 코드로 확인한다.
 * 아래 숫자는 서비스 정책이다. 근거 자료가 있는 값이 아니다(docs/decisions.md 2026-11-28 "자체 가입은 이메일 인증 코드를 받는다").
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationDomainService {

	/** 코드가 살아 있는 시간 */
	static final Duration CODE_TTL = Duration.ofMinutes(10);
	/** 같은 이메일로 다시 받기까지 기다리는 시간 */
	static final Duration COOLDOWN = Duration.ofMinutes(1);
	/** 한 코드로 틀릴 수 있는 횟수. 다 틀리면 코드를 지운다. 여섯 자리를 맞혀 볼 기회를 줄인다 */
	static final int MAX_FAILURES = 5;
	/** 한 이메일이 하루에 받을 수 있는 코드 수 */
	static final int DAILY_PER_EMAIL = 5;
	/** 서비스 전체가 하루에 보내는 메일 수. Resend 무료 요금제의 하루 100통(https://resend.com/pricing) 아래로 둔다 */
	static final int DAILY_TOTAL = 90;

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	private final EmailCodeRedisRepository repository;
	private final Clock clock;

	private final RandomGenerator random = new SecureRandom();

	/** 새 코드를 만들어 저장하고 돌려준다. 너무 자주, 너무 많이 받으면 429, 하루 발송량을 넘으면 503이다 */
	public String issue(String email) {

		String key = keyOf(email);
		if (!repository.startCooldown(key, COOLDOWN)) {
			throw new MemberException(EmailCodeErrorCode.TOO_SOON);
		}
		LocalDate today = LocalDate.now(clock.withZone(KOREA));
		if (repository.countDaily(key, today) > DAILY_PER_EMAIL) {
			throw new MemberException(EmailCodeErrorCode.DAILY_LIMIT);
		}
		if (repository.countSentToday(today) > DAILY_TOTAL) {
			throw new MemberException(EmailCodeErrorCode.MAIL_UNAVAILABLE);
		}

		String code = String.format("%06d", random.nextInt(1_000_000));
		repository.saveCode(key, code, CODE_TTL);
		return code;
	}

	/** 메일을 보내지 못했으면 코드를 지우고 바로 다시 받을 수 있게 한다 */
	public void cancel(String email) {
		String key = keyOf(email);
		repository.deleteCode(key);
		repository.clearCooldown(key);
	}

	/** 코드가 맞으면 지운다(한 번만 쓴다). 틀리면 400이고, 다섯 번 틀리면 코드를 지워 다시 받게 한다 */
	public void verify(String email, String code) {

		String key = keyOf(email);
		String saved = repository.findCode(key);
		if (saved == null) {
			throw new MemberException(EmailCodeErrorCode.CODE_EXPIRED);
		}
		String given = code == null ? "" : code.strip();
		if (!MessageDigest.isEqual(saved.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
			if (repository.addFailure(key, CODE_TTL) >= MAX_FAILURES) {
				repository.deleteCode(key);
				throw new MemberException(EmailCodeErrorCode.CODE_EXPIRED);
			}
			throw new MemberException(EmailCodeErrorCode.CODE_MISMATCH);
		}
		repository.deleteCode(key);
	}

	static String keyOf(String email) {
		return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
	}
}
