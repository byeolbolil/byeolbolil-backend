package xyz.ncookie.stargazer.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.security.jwt.JwtAuthenticationEntryPoint;
import xyz.ncookie.stargazer.global.security.jwt.JwtAuthenticationFilter;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.handler.OAuth2LoginFailureHandler;
import xyz.ncookie.stargazer.global.security.oauth.WithdrawalAuthorizationRequestResolver;
import xyz.ncookie.stargazer.global.security.oauth.WithdrawalAwareAuthorizationRequestRepository;
import xyz.ncookie.stargazer.global.security.service.CustomOAuth2UserService;
import xyz.ncookie.stargazer.global.security.handler.OAuth2LoginSuccessHandler;
import xyz.ncookie.stargazer.global.security.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomUserDetailsService userDetailsService;
	private final CustomOAuth2UserService customOAuth2UserService;

	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

	private final ClientRegistrationRepository clientRegistrationRepository;

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {

		http
			.csrf(AbstractHttpConfigurer::disable) 		// REST API이므로 CSRF 보안 필요 없음
			.formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 안 씀
			.httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 안 씀
			.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // H2 콘솔 허용
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))

			// 세션을 사용하지 않음 (Stateless)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// URL별 권한 관리
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()			// preflight 무조건 허용
				.requestMatchers("/", "/css/**", "/images/**", "/js/**", "/favicon.ico", "/h2-console/**").permitAll()

				// Swagger UI 경로 허용
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

				.requestMatchers("/api/v1/spots/**").permitAll()	// 관측지 조회
				.requestMatchers("/api/v1/recommendations/**").permitAll()	// 밤 추천은 로그인하지 않아도 기본 명소로 볼 수 있다
				.requestMatchers(HttpMethod.GET, "/api/v1/clouds/**").permitAll()	// 지도의 구름 레이어
				.requestMatchers(HttpMethod.GET, "/api/v1/geocode/**").permitAll()	// 출발 위치를 정할 때 쓰는 지명 검색
				.requestMatchers("/api/v1/auth/**", "/login/**", "/oauth2/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/members/exists/**").permitAll()	// 가입 전에 부르는 중복 검사

				.anyRequest().authenticated() // 나머지는 인증 필요
			)

			.userDetailsService(userDetailsService)

			// 소셜 로그인 설정
			// 로그인 주소에 탈퇴 표가 붙어 오면 콜백까지 들고 간다(docs/decisions.md 2026-11-28 "회원 탈퇴는 제공자 로그인을 한 번 더 거친다")
			.oauth2Login(oauth2 -> oauth2
					.authorizationEndpoint(endpoint -> endpoint
						.authorizationRequestResolver(new WithdrawalAuthorizationRequestResolver(clientRegistrationRepository))
						.authorizationRequestRepository(new WithdrawalAwareAuthorizationRequestRepository()))
					.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
					.successHandler(oAuth2LoginSuccessHandler)
					.failureHandler(oAuth2LoginFailureHandler)
			)

			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
		);

		http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(List.of(
			"http://localhost:3000",
			"http://localhost:8080",      // 로컬 Swagger
			"https://www.byeolbolil.com",
			"https://api.byeolbolil.com", // 프로덕션 Swagger
			// 옛 도메인. .xyz를 새 주소로 넘기기 전에 열려 있던 화면을 위해 남긴다(docs/decisions.md 2026-11-28)
			"https://www.byeolbolil.xyz",
			"https://api.byeolbolil.xyz"
		));

		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		// 허용할 헤더
		config.setAllowedHeaders(List.of("*"));

		// 쿠키 주고받기 허용
		config.setAllowCredentials(true);

		// 클라이언트가 읽을 수 있는 헤더
		config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
