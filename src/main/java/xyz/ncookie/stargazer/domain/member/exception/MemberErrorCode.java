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
	ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 할 수 있습니다."),
	// 탈퇴. 비밀번호가 틀려도 401이 아니다. 클라이언트는 401을 받으면 토큰을 재발급하기 때문이다
	PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 맞지 않습니다."),
	WITHDRAWAL_NEEDS_PROVIDER(HttpStatus.BAD_REQUEST, "소셜 로그인 회원은 가입한 서비스에 다시 로그인해서 탈퇴합니다."),
	WITHDRAWAL_NEEDS_PASSWORD(HttpStatus.BAD_REQUEST, "이메일로 가입한 회원은 비밀번호를 넣어 탈퇴합니다."),
	WITHDRAWAL_REJECTED(HttpStatus.BAD_REQUEST, "탈퇴를 확인하지 못했습니다. 가입한 계정으로 다시 시도해 주세요.")
	;

	private final HttpStatus status;
	private final String message;
}
