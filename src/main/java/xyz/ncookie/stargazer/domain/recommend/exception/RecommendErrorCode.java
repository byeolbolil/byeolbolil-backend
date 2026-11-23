package xyz.ncookie.stargazer.domain.recommend.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum RecommendErrorCode implements ErrorCode {

	DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "오늘 밤부터 7일 안의 날짜만 볼 수 있습니다."),
	;

	private final HttpStatus status;
	private final String message;
}
