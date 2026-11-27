package xyz.ncookie.stargazer.domain.member.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
	MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
	INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 쓰는 닉네임입니다."),
	ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 할 수 있습니다.")
	;

	private final HttpStatus status;
	private final String message;
}
