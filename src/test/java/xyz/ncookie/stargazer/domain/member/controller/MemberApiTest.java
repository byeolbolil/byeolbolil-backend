package xyz.ncookie.stargazer.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 내 정보 조회와 이메일 중복 검사의 응답을 고정한다.
 */
class MemberApiTest extends IntegrationTestSupport {

	private static final String EMAIL = "stargazer@example.com";

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private Member member;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		member = memberRepository.save(Member.local(EMAIL, "password", "별지기"));
	}

	@Nested
	@DisplayName("GET /api/v1/members/me")
	class Me {

		@Test
		@DisplayName("회원 ID와 닉네임을 준다")
		void myInfo() {
			MvcTestResult result = mvc.get().uri("/api/v1/members/me")
				.header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
				.exchange();

			assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": { "memberId": %d, "nickname": "별지기" }
				}
				""".formatted(member.getId()));
		}

		@Test
		@DisplayName("토큰이 없으면 401이다")
		void withoutToken() {
			assertThat(mvc.get().uri("/api/v1/members/me").exchange())
				.hasStatus(401).bodyJson().isStrictlyEqualTo("""
					{
					  "success": false,
					  "status": 401,
					  "message": "인증이 필요합니다."
					}
					""");
		}

		@Test
		@DisplayName("토큰이 깨졌거나 만료됐을 때도 토큰이 없을 때와 같은 401이다")
		void brokenToken() {
			MvcTestResult result = mvc.get().uri("/api/v1/members/me")
				.header("Authorization", "Bearer abc.def.ghi")
				.exchange();

			assertThat(result).hasStatus(401).bodyJson()
				.extractingPath("$.message").isEqualTo("인증이 필요합니다.");
		}
	}

	@Nested
	@DisplayName("GET /api/v1/members/exists/email")
	class EmailCheck {

		@Test
		@DisplayName("가입 전에 부르는 API라 로그인하지 않아도 된다. 쓸 수 있는 이메일이면 validated가 true다")
		void available() {
			assertThat(check("new@example.com")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": { "validated": true }
				}
				""");
		}

		@Test
		@DisplayName("이미 가입된 이메일이면 validated가 false다")
		void taken() {
			assertThat(check(EMAIL)).hasStatusOk().bodyJson()
				.extractingPath("$.data.validated").isEqualTo(false);
		}

		@Test
		@DisplayName("email이 빠지면 400이다")
		void missingParameter() {
			assertThat(mvc.get().uri("/api/v1/members/exists/email").exchange())
				.hasStatus(400).bodyJson().isStrictlyEqualTo("""
					{
					  "success": false,
					  "status": 400,
					  "message": "Required parameter 'email' is not present."
					}
					""");
		}

		@Test
		@DisplayName("공개 API는 깨진 토큰이 붙어 와도 로그인하지 않은 요청으로 처리한다")
		void brokenTokenOnPublicApi() {
			MvcTestResult result = mvc.get().uri("/api/v1/members/exists/email")
				.param("email", "new@example.com")
				.header("Authorization", "Bearer abc.def.ghi")
				.exchange();

			assertThat(result).hasStatusOk();
		}

		private MvcTestResult check(String email) {
			return mvc.get().uri("/api/v1/members/exists/email").param("email", email).exchange();
		}
	}
}
