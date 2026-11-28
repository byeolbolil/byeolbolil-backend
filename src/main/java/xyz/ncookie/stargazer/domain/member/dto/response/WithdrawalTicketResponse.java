package xyz.ncookie.stargazer.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 회원의 탈퇴 주소")
public record WithdrawalTicketResponse(
	@Schema(description = "API 서버 기준 경로. 브라우저를 이 주소로 보내면 제공자 로그인을 거쳐 탈퇴하고 프론트로 돌아온다. "
		+ "5분 동안 한 번 쓸 수 있다", example = "/oauth2/authorization/kakao?withdraw=5f0c6c3e-0b1d-4a57-9d36-3c0b1f1f2a10")
	String authorizationUrl
) {
}
