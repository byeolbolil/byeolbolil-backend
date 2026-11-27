package xyz.ncookie.stargazer.domain.member.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum EmailCodeErrorCode implements ErrorCode {

	CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증 코드가 맞지 않습니다."),
	CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드를 다시 받아 주세요."),
	TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "인증 코드는 1분에 한 번 받을 수 있습니다."),
	DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘은 인증 코드를 더 받을 수 없습니다. 내일 다시 시도해 주세요."),
	MAIL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.")
	;

	private final HttpStatus status;
	private final String message;
}
