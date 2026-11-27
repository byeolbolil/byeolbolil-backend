package xyz.ncookie.stargazer.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 닉네임 규칙은 {@code MemberDomainService}가 검사한다. 규칙에 어긋나면 400이다.
 */
@Schema(description = "내 정보 수정 요청")
public record ModifyMemberRequest(
	@Schema(
		description = "새 닉네임. 앞뒤 공백을 떼고 2~16자, 한글·영문·숫자·밑줄과 낱말 사이의 공백 하나만 쓸 수 있다",
		example = "별지기",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	String nickname
) {
}
