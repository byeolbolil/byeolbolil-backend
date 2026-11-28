package xyz.ncookie.stargazer.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 회원의 탈퇴 요청")
public record WithdrawMemberRequest(
	@Schema(description = "지금 비밀번호", example = "starry-night1!")
	String password
) {
}
