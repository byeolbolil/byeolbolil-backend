package xyz.ncookie.stargazer.domain.geocode.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class GeocodeException extends BaseException {

	public GeocodeException(ErrorCode errorCode) {
		super(errorCode);
	}
}
