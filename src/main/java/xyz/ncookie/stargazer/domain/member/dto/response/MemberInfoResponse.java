package xyz.ncookie.stargazer.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;

@Schema(description = "회원 정보 응답 DTO")
public record MemberInfoResponse(
	@Schema(description = "회원 ID", example = "1")
	Long memberId,
	
	@Schema(description = "닉네임", example = "별보러가자")
	String nickname,

	@Schema(description = "가입 방식. LOCAL이면 비밀번호로, 나머지는 제공자 로그인을 거쳐 탈퇴한다", example = "KAKAO")
	AuthProvider authProvider
) {

	public static MemberInfoResponse from(Member member) {

		return new MemberInfoResponse(member.getId(), member.getNickname(), member.getAuthProvider());
	}
}
