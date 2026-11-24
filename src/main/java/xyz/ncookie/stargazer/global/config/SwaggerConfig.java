package xyz.ncookie.stargazer.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("별볼일 API")
				.description("오늘 밤 어디서 언제 별을 보기 좋은지 알려 주는 별볼일의 API. "
					+ "공통 응답 형식, 인증 흐름, 오류 응답은 아래 문서에 정리돼 있다.")
				.version("v1"))
			.externalDocs(new ExternalDocumentation()
				.description("API 문서 (docs/API.md)")
				.url("https://github.com/byeolbolil/byeolbolil-backend/blob/dev/docs/API.md"))
			.servers(List.of(
				new Server().url("http://localhost:8080").description("로컬 개발 서버"),
				new Server().url("https://api.byeolbolil.xyz").description("프로덕션 서버")
			))
			.components(new Components()
				.addSecuritySchemes("bearer-jwt", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("Authorization")))
			.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}
}
