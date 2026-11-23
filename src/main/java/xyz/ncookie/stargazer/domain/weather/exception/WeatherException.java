package xyz.ncookie.stargazer.domain.weather.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class WeatherException extends BaseException {

	public WeatherException(ErrorCode errorCode) {
		super(errorCode);
	}

	public WeatherException(ErrorCode errorCode, String detailMessage) {
		super(errorCode, detailMessage);
	}
}
