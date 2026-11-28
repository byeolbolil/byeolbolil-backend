package xyz.ncookie.stargazer.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더. SecurityConfig에 두면 보안 설정이 소셜 로그인 서비스를 거쳐 회원 서비스(탈퇴 비밀번호 확인)에
 * 닿을 때 다시 SecurityConfig를 찾아 빈 순환이 생겨서 따로 둔다.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
