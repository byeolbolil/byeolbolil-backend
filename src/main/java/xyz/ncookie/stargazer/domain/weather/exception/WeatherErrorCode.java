package xyz.ncookie.stargazer.domain.weather.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum WeatherErrorCode implements ErrorCode {

	FORECAST_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "날씨 예보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."),
	;

	private final HttpStatus status;
	private final String message;
}
