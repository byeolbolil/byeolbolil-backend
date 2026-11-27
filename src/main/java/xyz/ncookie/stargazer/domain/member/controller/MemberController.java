package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import xyz.ncookie.stargazer.domain.member.application.dto.ModifyMemberCommand;
import xyz.ncookie.stargazer.domain.member.dto.request.ModifyMemberRequest;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberValidationResponse;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@Tag(name = "회원", description = "회원 정보 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberApplicationService memberApplicationService;

	@Operation(
		summary = "내 정보 조회",
		description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = MemberInfoResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@GetMapping("/me")
	public MemberInfoResponse getMyInfo(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal
	) {

		return memberApplicationService.getMyInfo(principal.getMemberId());
	}

	@Operation(
		summary = "내 정보 수정",
		description = "닉네임을 바꿉니다. 앞뒤 공백을 떼고 2~16자, 한글·영문·숫자·밑줄과 낱말 사이의 공백 하나만 쓸 수 있습니다. "
			+ "다른 회원과 같은 닉네임도 됩니다. 응답은 내 정보 조회와 같습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "수정 성공",
			content = @Content(schema = @Schema(implementation = MemberInfoResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "닉네임이 규칙에 어긋남"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@PatchMapping("/me")
	public MemberInfoResponse modifyMyInfo(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@RequestBody ModifyMemberRequest request
	) {

		return memberApplicationService.modifyMyInfo(principal.getMemberId(), ModifyMemberCommand.from(request));
	}

	@Operation(
		summary = "닉네임 중복 검사",
		description = "가입하거나 닉네임을 바꾸기 전에 쓸 수 있는 닉네임인지 확인합니다. 인증 없이 사용 가능합니다. "
			+ "로그인한 채로 부르면 자기 닉네임은 쓸 수 있다고 답합니다. 대소문자는 가리지 않습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "검증 완료 (validated: true면 쓸 수 있음)",
			content = @Content(schema = @Schema(implementation = MemberValidationResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "닉네임이 규칙에 어긋남"
		)
	})
	@GetMapping("/exists/nickname")
	public MemberValidationResponse checkNickname(
		@Parameter(hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "검사할 닉네임", example = "별지기", required = true)
		@RequestParam(value = "nickname") String nickname
	) {

		return memberApplicationService.checkNickname(nickname, principal == null ? null : principal.getMemberId());
	}

	@Operation(
		summary = "이메일 중복 검증",
		description = "회원가입 시 이메일 중복 여부를 확인합니다. 인증 없이 사용 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "검증 완료",
			content = @Content(schema = @Schema(implementation = MemberValidationResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (이메일 형식 오류 등)"
		)
	})
	@GetMapping("/exists/email")
	public MemberValidationResponse validateEmailDuplicated(
		@Parameter(description = "검증할 이메일 주소", example = "user@example.com", required = true)
		@RequestParam(value = "email") String email
	) {

		return memberApplicationService.validateEmailDuplicated(email);
	}
}
