package xyz.ncookie.stargazer.domain.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.member.exception.EmailCodeErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.EmailCodeRedisRepository;

/**
 * 인증 코드를 받는 횟수의 경계를 고정한다. 숫자는 정책이다(docs/decisions.md 2026-11-28 "자체 가입은 이메일 인증 코드를 받는다").
 */
class EmailVerificationDomainServiceTest {

	// 한국 시각으로 11월 28일 오전 8시 59분
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-27T23:59:00Z"), ZoneOffset.UTC);

	private final EmailCodeRedisRepository repository = mock(EmailCodeRedisRepository.class);
	private final EmailVerificationDomainService service = new EmailVerificationDomainService(repository, CLOCK);

	@BeforeEach
	void setUp() {
		when(repository.startCooldown(anyString(), any())).thenReturn(true);
		when(repository.countDaily(anyString(), any())).thenReturn(1L);
		when(repository.countSentToday(any())).thenReturn(1L);
	}

	@Test
	@DisplayName("여섯 자리 코드를 10분 동안 둔다. 하루는 한국 날짜로 센다")
	void issues() {
		String code = service.issue("a@example.com");

		assertThat(code).matches("\\d{6}");
		verify(repository).saveCode("a@example.com", code, EmailVerificationDomainService.CODE_TTL);
		verify(repository).countDaily("a@example.com", LocalDate.of(2026, 11, 28));
	}

	@Test
	@DisplayName("한 이메일은 하루 다섯 번까지 받고 여섯 번째는 429다")
	void dailyPerEmail() {
		when(repository.countDaily(anyString(), any())).thenReturn(5L);
		service.issue("a@example.com");

		when(repository.countDaily(anyString(), any())).thenReturn(6L);
		assertThatThrownBy(() -> service.issue("a@example.com"))
			.extracting(e -> ((MemberException)e).getErrorCode())
			.isEqualTo(EmailCodeErrorCode.DAILY_LIMIT);
	}

	@Test
	@DisplayName("서비스 전체로 하루 90통을 넘기면 보내지 않고 503이다")
	void dailyTotal() {
		when(repository.countSentToday(any())).thenReturn(90L);
		service.issue("a@example.com");

		when(repository.countSentToday(any())).thenReturn(91L);
		assertThatThrownBy(() -> service.issue("b@example.com"))
			.extracting(e -> ((MemberException)e).getErrorCode())
			.isEqualTo(EmailCodeErrorCode.MAIL_UNAVAILABLE);
	}

	@Test
	@DisplayName("대기 중이면 횟수를 세지 않고 429다")
	void cooldown() {
		when(repository.startCooldown(anyString(), any())).thenReturn(false);

		assertThatThrownBy(() -> service.issue("a@example.com"))
			.extracting(e -> ((MemberException)e).getErrorCode())
			.isEqualTo(EmailCodeErrorCode.TOO_SOON);
		verify(repository, never()).countDaily(anyString(), any());
	}

	@Test
	@DisplayName("다섯 번째로 틀리면 코드를 지운다")
	void fifthFailureDeletes() {
		when(repository.findCode("a@example.com")).thenReturn("123456");
		when(repository.addFailure(eq("a@example.com"), any())).thenReturn(4L, 5L);

		assertThatThrownBy(() -> service.verify("a@example.com", "000000"))
			.extracting(e -> ((MemberException)e).getErrorCode())
			.isEqualTo(EmailCodeErrorCode.CODE_MISMATCH);
		verify(repository, never()).deleteCode(anyString());

		assertThatThrownBy(() -> service.verify("a@example.com", "000000"))
			.extracting(e -> ((MemberException)e).getErrorCode())
			.isEqualTo(EmailCodeErrorCode.CODE_EXPIRED);
		verify(repository).deleteCode("a@example.com");
	}

	@Test
	@DisplayName("맞으면 코드를 지운다. 앞뒤 공백과 이메일의 대소문자는 가리지 않는다")
	void verifies() {
		when(repository.findCode("a@example.com")).thenReturn("123456");

		service.verify(" A@Example.com", " 123456 ");

		verify(repository).deleteCode("a@example.com");
	}
}
