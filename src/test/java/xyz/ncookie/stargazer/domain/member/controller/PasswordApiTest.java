package xyz.ncookie.stargazer.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import jakarta.servlet.http.Cookie;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.EmailCodeRedisRepository;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 비밀번호 찾기와 변경, 로그인 시도 제한. 비밀번호를 바꾸면 모든 기기의 로그인이 끊긴다.
 */
class PasswordApiTest extends IntegrationTestSupport {

	private static final String EMAIL = "stargazer@example.com";
	private static final String PASSWORD = "starry-night1!";
	private static final String NEW_PASSWORD = "milky-way2@";
	private static final String RESET_KEY = "password-reset:" + EMAIL;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private EmailCodeRedisRepository emailCodeRedisRepository;

	@Autowired
	private RefreshTokenRedisRepository refreshTokenRedisRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Member member;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		member = memberRepository.save(Member.local(EMAIL, passwordEncoder.encode(PASSWORD), "별지기"));
	}

	@Nested
	@DisplayName("POST /api/v1/auth/password-reset/code")
	class ResetCode {

		@Test
		@DisplayName("이메일 회원이면 재설정 코드를 보낸다. 가입 코드와는 다른 자리에 둔다")
		void localMember() {
			externalApi.resendAccepts();

			assertThat(sendResetCode(EMAIL)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
				""");
			externalApi.verifyAllCalled();
			assertThat(emailCodeRedisRepository.findCode(RESET_KEY)).matches("\\d{6}");
			assertThat(emailCodeRedisRepository.findCode(EMAIL)).isNull();
		}

		@Test
		@DisplayName("가입하지 않은 이메일도 같은 200이고 메일은 보내지 않는다")
		void unknownEmail() {
			// Resend를 대체해 두지 않았으므로 메일을 보내려 하면 테스트가 실패한다
			assertThat(sendResetCode("nobody@example.com")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
				""");
		}

		@Test
		@DisplayName("소셜 로그인으로만 쓰는 이메일이면 같은 200이고, 어느 서비스로 가입했는지 알리는 메일을 보낸다")
		void socialOnlyEmail() {
			memberRepository.save(Member.oauth("social@example.com", "카카오", AuthProvider.KAKAO, "kakao-1"));
			externalApi.resendAccepts();

			assertThat(sendResetCode("social@example.com")).hasStatusOk();
			externalApi.verifyAllCalled();
		}

		@Test
		@DisplayName("1분 안에 다시 받으면 가입 여부와 상관없이 429다")
		void tooSoon() {
			sendResetCode("nobody@example.com");

			assertThat(sendResetCode("nobody@example.com")).hasStatus(429);
		}
	}

	@Nested
	@DisplayName("POST /api/v1/auth/password-reset")
	class Reset {

		@Test
		@DisplayName("코드가 맞으면 새 비밀번호로 바꾸고 모든 기기의 로그인을 끊는다")
		void resets() {
			String otherDevice = refreshTokenOf(member);
			emailCodeRedisRepository.saveCode(RESET_KEY, "482913", Duration.ofMinutes(10));

			assertThat(reset(EMAIL, "482913", NEW_PASSWORD)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
				""");
			assertThat(reissue(otherDevice)).hasStatus(401);
			assertThat(login(EMAIL, PASSWORD)).hasStatus(401);
			assertThat(login(EMAIL, NEW_PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("코드는 한 번만 쓴다")
		void codeIsSingleUse() {
			emailCodeRedisRepository.saveCode(RESET_KEY, "482913", Duration.ofMinutes(10));
			reset(EMAIL, "482913", NEW_PASSWORD);

			assertThat(reset(EMAIL, "482913", "another-pass3#")).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("인증 코드를 다시 받아 주세요.");
		}

		@Test
		@DisplayName("가입 인증 코드로는 비밀번호를 바꿀 수 없다")
		void signupCodeDoesNotWork() {
			emailCodeRedisRepository.saveCode(EMAIL, "482913", Duration.ofMinutes(10));

			assertThat(reset(EMAIL, "482913", NEW_PASSWORD)).hasStatus(400);
			assertThat(login(EMAIL, PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("코드가 틀리면 400이고 비밀번호는 그대로다")
		void wrongCode() {
			emailCodeRedisRepository.saveCode(RESET_KEY, "482913", Duration.ofMinutes(10));

			assertThat(reset(EMAIL, "000000", NEW_PASSWORD)).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("인증 코드가 맞지 않습니다.");
			assertThat(login(EMAIL, PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("새 비밀번호가 가입 규칙에 어긋나면 400이고 코드는 남는다")
		void weakPassword() {
			emailCodeRedisRepository.saveCode(RESET_KEY, "482913", Duration.ofMinutes(10));

			assertThat(reset(EMAIL, "482913", "short")).hasStatus(400).bodyJson()
				.extractingPath("$.data.newPassword").isEqualTo("비밀번호는 8자 이상, 문자/숫자/특수문자를 포함해야 합니다.");
			assertThat(reset(EMAIL, "482913", NEW_PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("막혀 있던 로그인도 풀린다")
		void unlocksLogin() {
			for (int i = 0; i < 5; i++) {
				login(EMAIL, "wrong-pass1!");
			}
			emailCodeRedisRepository.saveCode(RESET_KEY, "482913", Duration.ofMinutes(10));

			reset(EMAIL, "482913", NEW_PASSWORD);

			assertThat(login(EMAIL, NEW_PASSWORD)).hasStatusOk();
		}
	}

	@Nested
	@DisplayName("PATCH /api/v1/members/me/password")
	class Change {

		@Test
		@DisplayName("지금 비밀번호가 맞으면 바꾸고, 다른 기기는 끊고 이 요청에는 새 토큰을 준다")
		void changes() {
			String otherDevice = refreshTokenOf(member);

			MvcTestResult result = change(member, PASSWORD, NEW_PASSWORD);

			assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data.accessToken").isNotNull();
			assertThat(result.getResponse().getHeader("Set-Cookie")).startsWith("refresh_token=");
			assertThat(reissue(otherDevice)).hasStatus(401);
			assertThat(login(EMAIL, NEW_PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("지금 비밀번호가 틀리면 400이고 그대로다. 401이 아니어서 클라이언트가 재발급하지 않는다")
		void wrongCurrent() {
			assertThat(change(member, "wrong-pass1!", NEW_PASSWORD)).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("비밀번호가 맞지 않습니다.");
			assertThat(login(EMAIL, PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("소셜 회원은 비밀번호가 없어 400이다")
		void socialMember() {
			Member social = memberRepository.save(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));

			assertThat(change(social, PASSWORD, NEW_PASSWORD)).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("소셜 로그인 회원은 비밀번호가 없습니다.");
		}

		@Test
		@DisplayName("토큰이 없으면 401이다")
		void withoutToken() {
			assertThat(mvc.patch().uri("/api/v1/members/me/password").contentType(MediaType.APPLICATION_JSON)
				.content("{}").exchange()).hasStatus(401);
		}
	}

	@Nested
	@DisplayName("로그인 시도 제한")
	class LoginLimit {

		@Test
		@DisplayName("네 번 틀린 뒤에는 아직 로그인할 수 있고, 성공하면 센 횟수가 지워진다")
		void fourFailures() {
			for (int i = 0; i < 4; i++) {
				assertThat(login(EMAIL, "wrong-pass1!")).hasStatus(401);
			}
			assertThat(login(EMAIL, PASSWORD)).hasStatusOk();

			for (int i = 0; i < 4; i++) {
				login(EMAIL, "wrong-pass1!");
			}
			assertThat(login(EMAIL, PASSWORD)).hasStatusOk();
		}

		@Test
		@DisplayName("다섯 번 틀리면 맞는 비밀번호도 429로 막는다. 대소문자만 다른 이메일도 같은 것으로 센다")
		void fiveFailures() {
			for (int i = 0; i < 5; i++) {
				login(i % 2 == 0 ? EMAIL : "Stargazer@Example.com", "wrong-pass1!");
			}

			assertThat(login(EMAIL, PASSWORD)).hasStatus(429).bodyJson().isStrictlyEqualTo("""
				{ "success": false, "status": 429, "message": "로그인을 여러 번 실패했습니다. 15분 뒤에 다시 시도해 주세요." }
				""");
		}

		@Test
		@DisplayName("다른 이메일은 막히지 않는다")
		void otherEmail() {
			for (int i = 0; i < 5; i++) {
				login("other@example.com", "wrong-pass1!");
			}

			assertThat(login(EMAIL, PASSWORD)).hasStatusOk();
		}
	}

	private MvcTestResult sendResetCode(String email) {
		return postJson("/api/v1/auth/password-reset/code", "{ \"email\": \"" + email + "\" }");
	}

	private MvcTestResult reset(String email, String code, String newPassword) {
		return postJson("/api/v1/auth/password-reset",
			"{ \"email\": \"" + email + "\", \"code\": \"" + code + "\", \"newPassword\": \"" + newPassword + "\" }");
	}

	private MvcTestResult login(String email, String password) {
		return postJson("/api/v1/auth/login", "{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }");
	}

	private MvcTestResult change(Member who, String current, String next) {
		return mvc.patch().uri("/api/v1/members/me/password")
			.header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(who.getId()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"currentPassword\": \"" + current + "\", \"newPassword\": \"" + next + "\" }")
			.exchange();
	}

	private MvcTestResult postJson(String uri, String body) {
		return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private MvcTestResult reissue(String refreshToken) {
		return mvc.post().uri("/api/v1/auth/reissue").cookie(new Cookie("refresh_token", refreshToken)).exchange();
	}

	private String refreshTokenOf(Member who) {
		String token = jwtTokenProvider.createRefreshToken(who.getId());
		refreshTokenRedisRepository.save(token, who.getId(), JwtTokenProvider.REFRESH_EXPIRE_MS);
		return token;
	}
}
