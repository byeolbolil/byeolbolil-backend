package xyz.ncookie.stargazer.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 재설정 요청")
public record ResetPasswordRequest(
	@Schema(description = "가입한 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@Email @NotBlank String email,

	@Schema(description = "이메일로 받은 여섯 자리 재설정 코드", example = "482913", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank String code,

	@Schema(
		description = "새 비밀번호 (8자 이상, 영문자/숫자/특수문자(!@#$%^&*) 각각 최소 1개 이상 포함). 회원가입과 같은 규칙",
		example = "password123!",
		requiredMode = Schema.RequiredMode.REQUIRED,
		pattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"
	)
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
		message = "비밀번호는 8자 이상, 문자/숫자/특수문자를 포함해야 합니다."
	)
	@NotBlank String newPassword
) {
}
