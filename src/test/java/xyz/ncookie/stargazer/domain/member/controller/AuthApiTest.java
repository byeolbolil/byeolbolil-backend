package xyz.ncookie.stargazer.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 가입, 로그인, 재발급, 로그아웃이 내보내는 응답과 토큰의 수명 주기를 고정한다.
 *
 * 액세스 토큰은 응답 본문으로, 리프레시 토큰은 refresh_token 쿠키로 나간다.
 * 리프레시 토큰은 Redis에 있는 것만 통하고 한 번 쓰면 새것으로 바뀐다.
 */
class AuthApiTest extends IntegrationTestSupport {

	private static final String EMAIL = "stargazer@example.com";
	private static final String PASSWORD = "starry-night1!";
	private static final String NICKNAME = "별지기";

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("POST /api/v1/auth/register")
	class Register {

		@Test
		@DisplayName("가입하면 바로 로그인된 상태로 두 토큰을 준다")
		void issuesTokens() {
			MvcTestResult result = register(EMAIL, PASSWORD, NICKNAME);

			assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다."
				}
				""");
			assertThat(accessTokenOf(result)).isNotBlank();
			assertThat(result.getResponse().getHeader("Set-Cookie"))
				.startsWith("refresh_token=")
				.contains("Path=/", "Max-Age=1209600", "Secure", "HttpOnly", "SameSite=None");
		}

		@Test
		@DisplayName("가입할 때 받은 리프레시 토큰으로 재발급할 수 있다")
		void refreshTokenWorks() {
			String refreshToken = refreshTokenOf(register(EMAIL, PASSWORD, NICKNAME));

			assertThat(reissueWithCookie(refreshToken)).hasStatusOk();
		}
	}

	@Nested
	@DisplayName("POST /api/v1/auth/login")
	class Login {

		@BeforeEach
		void member() {
			register(EMAIL, PASSWORD, NICKNAME);
		}

		@Test
		@DisplayName("로그인하면 두 토큰을 준다")
		void issuesTokens() {
			MvcTestResult result = login(EMAIL, PASSWORD);

			assertThat(result).hasStatusOk();
			assertThat(accessTokenOf(result)).isNotBlank();
			assertThat(result.getResponse().getHeader("Set-Cookie")).startsWith("refresh_token=");
		}

		@Test
		@DisplayName("받은 액세스 토큰으로 인증이 필요한 API를 부를 수 있다")
		void accessTokenWorks() {
			String accessToken = accessTokenOf(login(EMAIL, PASSWORD));

			assertThat(me(accessToken)).hasStatusOk().bodyJson()
				.extractingPath("$.data.nickname").isEqualTo(NICKNAME);
		}

		@Test
		@DisplayName("리프레시 토큰은 액세스 토큰 자리에서 통하지 않는다")
		void refreshTokenIsNotAnAccessToken() {
			String refreshToken = refreshTokenOf(login(EMAIL, PASSWORD));

			assertThat(me(refreshToken)).hasStatus(401);
		}
	}

	@Nested
	@DisplayName("POST /api/v1/auth/reissue")
	class Reissue {

		private String refreshToken;

		@BeforeEach
		void loggedIn() {
			register(EMAIL, PASSWORD, NICKNAME);
			refreshToken = refreshTokenOf(login(EMAIL, PASSWORD));
		}

		@Test
		@DisplayName("쿠키의 리프레시 토큰으로 두 토큰을 새로 받는다")
		void withCookie() {
			MvcTestResult result = reissueWithCookie(refreshToken);

			assertThat(result).hasStatusOk();
			assertThat(me(accessTokenOf(result))).hasStatusOk();
			assertThat(refreshTokenOf(result)).isNotEqualTo(refreshToken);
		}

		@Test
		@DisplayName("쿠키를 쓰기 어려운 클라이언트는 Authorization-Refresh 헤더로 보낼 수 있다")
		void withHeader() {
			MvcTestResult result = mvc.post().uri("/api/v1/auth/reissue")
				.header("Authorization-Refresh", "Bearer " + refreshToken)
				.exchange();

			assertThat(result).hasStatusOk();
			assertThat(accessTokenOf(result)).isNotBlank();
		}

		@Test
		@DisplayName("한 번 쓴 리프레시 토큰은 다시 쓸 수 없고, 새로 받은 것만 통한다")
		void rotates() {
			String rotated = refreshTokenOf(reissueWithCookie(refreshToken));

			assertThat(reissueWithCookie(refreshToken)).hasStatus(401).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 401,
				  "message": "유효하지 않은 Refresh Token 입니다."
				}
				""");
			assertThat(reissueWithCookie(rotated)).hasStatusOk();
		}

		@Test
		@DisplayName("리프레시 토큰 없이 부르면 401이다")
		void withoutToken() {
			assertThat(mvc.post().uri("/api/v1/auth/reissue").exchange()).hasStatus(401);
		}
	}

	@Nested
	@DisplayName("POST /api/v1/auth/logout")
	class Logout {

		@Test
		@DisplayName("리프레시 토큰을 없애고 쿠키를 지운다. data는 없다")
		void revokes() {
			String refreshToken = refreshTokenOf(register(EMAIL, PASSWORD, NICKNAME));

			MvcTestResult result = mvc.post().uri("/api/v1/auth/logout")
				.cookie(new Cookie("refresh_token", refreshToken))
				.exchange();

			assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다."
				}
				""");
			assertThat(result.getResponse().getHeader("Set-Cookie"))
				.startsWith("refresh_token=;")
				.contains("Max-Age=0");
			assertThat(reissueWithCookie(refreshToken)).hasStatus(401);
		}

		@Test
		@DisplayName("리프레시 토큰이 없어도 200이다")
		void withoutToken() {
			assertThat(mvc.post().uri("/api/v1/auth/logout").exchange()).hasStatusOk();
		}
	}

	// ---- 준비 ----

	private MvcTestResult register(String email, String password, String nickname) {
		return postJson("/api/v1/auth/register", """
			{ "email": "%s", "password": "%s", "nickname": "%s" }
			""".formatted(email, password, nickname));
	}

	private MvcTestResult login(String email, String password) {
		return postJson("/api/v1/auth/login", """
			{ "email": "%s", "password": "%s" }
			""".formatted(email, password));
	}

	private MvcTestResult postJson(String uri, String body) {
		return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private MvcTestResult reissueWithCookie(String refreshToken) {
		return mvc.post().uri("/api/v1/auth/reissue")
			.cookie(new Cookie("refresh_token", refreshToken))
			.exchange();
	}

	private MvcTestResult me(String token) {
		return mvc.get().uri("/api/v1/members/me").header("Authorization", "Bearer " + token).exchange();
	}

	private static String accessTokenOf(MvcTestResult result) {
		try {
			return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.accessToken");
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Set-Cookie의 refresh_token 값. 네이티브 앱도 이 헤더에서 토큰을 얻는다. */
	private static String refreshTokenOf(MvcTestResult result) {
		String setCookie = result.getResponse().getHeader("Set-Cookie");
		assertThat(setCookie).startsWith("refresh_token=");
		return setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));
	}
}
