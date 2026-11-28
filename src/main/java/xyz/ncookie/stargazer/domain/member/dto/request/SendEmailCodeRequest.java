package xyz.ncookie.stargazer.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "가입 인증 코드 요청")
public record SendEmailCodeRequest(
	@Schema(description = "가입할 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@Email @NotBlank String email
) {
}
