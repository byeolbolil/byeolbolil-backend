package xyz.ncookie.stargazer.domain.geocode.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum GeocodeErrorCode implements ErrorCode {

	GEOCODE_BUSY(HttpStatus.TOO_MANY_REQUESTS, "지명 검색 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
	GEOCODE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "지명 검색을 지금 쓸 수 없습니다. 잠시 후 다시 시도해 주세요."),
	;

	private final HttpStatus status;
	private final String message;
}
