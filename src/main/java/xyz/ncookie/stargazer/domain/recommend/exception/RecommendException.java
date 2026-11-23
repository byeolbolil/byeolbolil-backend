package xyz.ncookie.stargazer.domain.recommend.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class RecommendException extends BaseException {

	public RecommendException(ErrorCode errorCode) {
		super(errorCode);
	}

	public RecommendException(ErrorCode errorCode, String detailMessage) {
		super(errorCode, detailMessage);
	}
}
