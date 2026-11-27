package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.application.MemberApplicationService;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

/**
 * 운영자가 쓰는 회원 관리 API. 액세스 토큰의 역할은 늘 USER라서, 관리자인지는 DB의 회원 역할로 본다.
 */
@Tag(name = "회원 관리", description = "관리자 전용 API")
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class MemberAdminController {

	private final MemberApplicationService memberApplicationService;

	@Operation(
		summary = "닉네임 초기화",
		description = "선정적이거나 광고인 닉네임을 \"별지기1234\" 꼴의 기본 닉네임으로 되돌립니다. 관리자만 쓸 수 있습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "되돌린 회원의 정보",
			content = @Content(schema = @Schema(implementation = MemberInfoResponse.class))
		),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "403", description = "관리자가 아님"),
		@ApiResponse(responseCode = "404", description = "없는 회원")
	})
	@SecurityRequirement(name = "bearer-jwt")
	@PostMapping("/{memberId}/nickname/reset")
	public MemberInfoResponse resetNickname(
		@Parameter(hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "닉네임을 되돌릴 회원 ID", example = "12", required = true)
		@PathVariable Long memberId
	) {

		return memberApplicationService.resetNickname(principal.getMemberId(), memberId);
	}
}
