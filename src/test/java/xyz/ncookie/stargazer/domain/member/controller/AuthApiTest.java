package xyz.ncookie.stargazer.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.EmailCodeRedisRepository;
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
	private static final String EMAIL_CODE = "482913";

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private EmailCodeRedisRepository emailCodeRedisRepository;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("POST /api/v1/auth/email-code")
	class EmailCode {

		@Test
		@DisplayName("여섯 자리 코드를 만들어 Resend로 보낸다. 받은 코드로 가입하면 된다")
		void sendsCode() {
			externalApi.resendAccepts();

			assertThat(sendCode(EMAIL)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
				""");
			externalApi.verifyAllCalled();
			String code = emailCodeRedisRepository.findCode(EMAIL);
			assertThat(code).matches("\\d{6}");

			assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, code)).hasStatusOk();
		}

		@Test
		@DisplayName("대소문자가 달라도 같은 이메일로 본다")
		void normalizesEmail() {
			externalApi.resendAccepts();
			sendCode("Stargazer@Example.com");

			assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, emailCodeRedisRepository.findCode(EMAIL))).hasStatusOk();
		}

		@Test
		@DisplayName("1분 안에 다시 받으면 429다")
		void tooSoon() {
			externalApi.resendAccepts();
			sendCode(EMAIL);

			assertThat(sendCode(EMAIL)).hasStatus(429).bodyJson()
				.extractingPath("$.message").isEqualTo("인증 코드는 1분에 한 번 받을 수 있습니다.");
		}

		@Test
		@DisplayName("이미 가입된 이메일이면 보내지 않고 409다")
		void alreadyRegistered() {
			register(EMAIL, PASSWORD, NICKNAME);

			assertThat(sendCode(EMAIL)).hasStatus(409).bodyJson()
				.extractingPath("$.message").isEqualTo("이미 사용 중인 이메일입니다.");
		}

		@Test
		@DisplayName("메일을 보내지 못하면 503이고, 코드를 지워 바로 다시 받을 수 있다")
		void mailFails() {
			externalApi.resendFails();

			assertThat(sendCode(EMAIL)).hasStatus(503).bodyJson()
				.extractingPath("$.message").isEqualTo("인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.");
			assertThat(emailCodeRedisRepository.findCode(EMAIL)).isNull();

			externalApi.reset();
			externalApi.resendAccepts();
			assertThat(sendCode(EMAIL)).hasStatusOk();
		}

		@Test
		@DisplayName("이메일 형식이 아니면 400이다")
		void invalidEmail() {
			assertThat(sendCode("not-an-email")).hasStatus(400);
		}

		private MvcTestResult sendCode(String email) {
			return postJson("/api/v1/auth/email-code", """
				{ "email": "%s" }
				""".formatted(email));
		}
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

		@Test
		@DisplayName("이미 가입된 이메일이면 409다")
		void duplicateEmail() {
			register(EMAIL, PASSWORD, NICKNAME);

			assertThat(register(EMAIL, PASSWORD, "다른 사람")).hasStatus(409).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 409,
				  "message": "이미 사용 중인 이메일입니다."
				}
				""");
		}

		@Test
		@DisplayName("다른 회원이 쓰는 닉네임이면 대소문자가 달라도 409다")
		void duplicateNickname() {
			register(EMAIL, PASSWORD, "Stargazer");

			assertThat(register("other@example.com", PASSWORD, "stargazer")).hasStatus(409).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 409,
				  "message": "이미 쓰는 닉네임입니다."
				}
				""");
		}

		@Test
		@DisplayName("인증 코드를 받지 않았거나 시간이 지났으면 400이다")
		void withoutCode() {
			assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, EMAIL_CODE)).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "인증 코드를 다시 받아 주세요."
				}
				""");
			assertThat(memberRepository.count()).isZero();
		}

		@Test
		@DisplayName("코드가 틀리면 400이고, 다섯 번 틀리면 코드가 지워져 맞는 코드도 통하지 않는다")
		void wrongCode() {
			emailCodeRedisRepository.saveCode(EMAIL, EMAIL_CODE, Duration.ofMinutes(10));

			for (int i = 1; i < 5; i++) {
				assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, "000000")).hasStatus(400).bodyJson()
					.extractingPath("$.message").isEqualTo("인증 코드가 맞지 않습니다.");
			}
			assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, "000000")).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("인증 코드를 다시 받아 주세요.");
			assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, EMAIL_CODE)).hasStatus(400);
		}

		@Test
		@DisplayName("코드는 한 번만 쓴다. 닉네임이 겹쳐 실패하면 코드는 남아 고쳐서 다시 보낼 수 있다")
		void codeUsedOnce() {
			memberRepository.save(Member.local("other@example.com", "pw", "은하수"));
			emailCodeRedisRepository.saveCode(EMAIL, EMAIL_CODE, Duration.ofMinutes(10));

			assertThat(registerWithCode(EMAIL, PASSWORD, "은하수", EMAIL_CODE)).hasStatus(409);
			assertThat(registerWithCode(EMAIL, PASSWORD, NICKNAME, EMAIL_CODE)).hasStatusOk();
			assertThat(emailCodeRedisRepository.findCode(EMAIL)).isNull();
		}

		@Test
		@DisplayName("닉네임 수정과 같은 규칙을 본다. 어긋나면 400이고, 앞뒤 공백은 떼고 저장한다")
		void nicknameRule() {
			assertThat(register(EMAIL, PASSWORD, "별지기⭐")).hasStatus(400).bodyJson()
				.extractingPath("$.message")
				.isEqualTo("닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다.");

			assertThat(register(EMAIL, PASSWORD, "  밤하늘 산책 ")).hasStatusOk();
			assertThat(memberRepository.findByEmail(EMAIL)).get()
				.extracting(Member::getNickname).isEqualTo("밤하늘 산책");
		}

		@Test
		@DisplayName("비밀번호 규칙에 맞지 않으면 400이고 data에 필드별 사유가 담긴다")
		void weakPassword() {
			assertThat(register(EMAIL, "short", NICKNAME)).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "유효성 검사에 실패하였습니다.",
				  "data": { "password": "비밀번호는 8자 이상, 문자/숫자/특수문자를 포함해야 합니다." }
				}
				""");
		}

		@Test
		@DisplayName("문구를 따로 정하지 않은 검증의 사유는 Accept-Language를 따른다")
		void defaultMessagesFollowLocale() {
			MvcTestResult korean = mvc.post().uri("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "ko-KR")
				.content("""
					{ "email": "not-an-email", "password": "%s", "nickname": "" }
					""".formatted(PASSWORD))
				.exchange();
			MvcTestResult english = mvc.post().uri("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "en-US")
				.content("""
					{ "email": "not-an-email", "password": "%s", "nickname": "" }
					""".formatted(PASSWORD))
				.exchange();

			assertThat(korean).hasStatus(400).bodyJson().isLenientlyEqualTo("""
				{ "data": { "email": "올바른 형식의 이메일 주소여야 합니다", "nickname": "공백일 수 없습니다" } }
				""");
			assertThat(english).hasStatus(400).bodyJson().isLenientlyEqualTo("""
				{ "data": { "email": "must be a well-formed email address", "nickname": "must not be blank" } }
				""");
		}

		@Test
		@DisplayName("본문이 JSON으로 읽히지 않으면 400이다")
		void brokenJson() {
			assertThat(postJson("/api/v1/auth/register", "{")).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "요청 본문을 읽을 수 없습니다."
				}
				""");
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
		@DisplayName("비밀번호가 틀리면 401이다")
		void wrongPassword() {
			assertThat(login(EMAIL, "wrong-password1!")).hasStatus(401).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 401,
				  "message": "이메일 또는 비밀번호가 일치하지 않습니다."
				}
				""");
		}

		@Test
		@DisplayName("가입하지 않은 이메일도 비밀번호가 틀렸을 때와 같은 응답이다")
		void unknownEmail() {
			assertThat(login("nobody@example.com", PASSWORD)).hasStatus(401).bodyJson()
				.extractingPath("$.message").isEqualTo("이메일 또는 비밀번호가 일치하지 않습니다.");
		}

		@Test
		@DisplayName("소셜 로그인으로만 가입한 회원은 비밀번호가 없어 이메일 로그인이 안 된다")
		void socialOnlyMember() {
			memberRepository.save(Member.oauth("social@example.com", "소셜", AuthProvider.KAKAO, "kakao-1"));

			assertThat(login("social@example.com", PASSWORD)).hasStatus(401);
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

	/** 인증 메일을 받은 것으로 치고 그 코드로 가입한다. 코드를 받는 흐름은 EmailCode에서 따로 본다 */
	private MvcTestResult register(String email, String password, String nickname) {
		emailCodeRedisRepository.saveCode(email.strip().toLowerCase(), EMAIL_CODE, Duration.ofMinutes(10));
		return registerWithCode(email, password, nickname, EMAIL_CODE);
	}

	private MvcTestResult registerWithCode(String email, String password, String nickname, String code) {
		return postJson("/api/v1/auth/register", """
			{ "email": "%s", "password": "%s", "nickname": "%s", "emailCode": "%s" }
			""".formatted(email, password, nickname, code));
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
