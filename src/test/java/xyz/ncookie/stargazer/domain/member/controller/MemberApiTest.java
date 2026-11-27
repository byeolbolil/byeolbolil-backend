package xyz.ncookie.stargazer.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.entity.Role;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 내 정보 조회와 수정, 이메일 중복 검사의 응답을 고정한다.
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
	@DisplayName("PATCH /api/v1/members/me")
	class Modify {

		@Test
		@DisplayName("닉네임을 바꾸고 내 정보 조회와 같은 모양으로 돌려준다")
		void changesNickname() {
			assertThat(modify(token(member), """
				{ "nickname": "밤하늘 산책" }
				""")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": { "memberId": %d, "nickname": "밤하늘 산책" }
				}
				""".formatted(member.getId()));
		}

		@Test
		@DisplayName("바꾼 닉네임은 앞뒤 공백을 뗀 채로 남아 다시 조회해도 보인다")
		void persists() {
			modify(token(member), """
				{ "nickname": "  별지기2  " }
				""");

			assertThat(mvc.get().uri("/api/v1/members/me").header("Authorization", token(member)).exchange())
				.hasStatusOk().bodyJson().extractingPath("$.data.nickname").isEqualTo("별지기2");
			assertThat(memberRepository.findById(member.getId())).get()
				.extracting(Member::getNickname).isEqualTo("별지기2");
		}

		@Test
		@DisplayName("다른 회원이 쓰는 닉네임이면 대소문자가 달라도 409이고 닉네임은 그대로다")
		void duplicate() {
			memberRepository.save(Member.local("other@example.com", "password", "Milky Way"));

			assertThat(modify(token(member), """
				{ "nickname": "milky way" }
				""")).hasStatus(409).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 409,
				  "message": "이미 쓰는 닉네임입니다."
				}
				""");
			assertThat(memberRepository.findById(member.getId())).get()
				.extracting(Member::getNickname).isEqualTo("별지기");
		}

		@Test
		@DisplayName("자기 닉네임의 대소문자만 바꾸는 것은 된다")
		void ownNicknameCase() {
			member.changeNickname("stargazer");
			memberRepository.save(member);

			assertThat(modify(token(member), """
				{ "nickname": "Stargazer" }
				""")).hasStatusOk().bodyJson().extractingPath("$.data.nickname").isEqualTo("Stargazer");
		}

		@Test
		@DisplayName("규칙에 어긋나면 400이고 닉네임은 그대로다")
		void invalid() {
			assertThat(modify(token(member), """
				{ "nickname": "별" }
				""")).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다."
				}
				""");
			assertThat(memberRepository.findById(member.getId())).get()
				.extracting(Member::getNickname).isEqualTo("별지기");
		}

		@Test
		@DisplayName("nickname이 빠져도 같은 400이다")
		void missing() {
			assertThat(modify(token(member), "{}")).hasStatus(400).bodyJson()
				.extractingPath("$.message")
				.isEqualTo("닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다.");
		}

		@Test
		@DisplayName("토큰이 없으면 401이다")
		void withoutToken() {
			assertThat(mvc.patch().uri("/api/v1/members/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "nickname": "별지기" }
					""")
				.exchange())
				.hasStatus(401).bodyJson().extractingPath("$.message").isEqualTo("인증이 필요합니다.");
		}

		private MvcTestResult modify(String authorization, String body) {
			return mvc.patch().uri("/api/v1/members/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
		}

		private String token(Member target) {
			return "Bearer " + jwtTokenProvider.createAccessToken(target.getId());
		}
	}

	@Nested
	@DisplayName("GET /api/v1/members/exists/nickname")
	class NicknameCheck {

		@Test
		@DisplayName("로그인하지 않아도 부를 수 있다. 아무도 안 쓰면 validated가 true다")
		void available() {
			assertThat(check("밤하늘", null)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": { "validated": true }
				}
				""");
		}

		@Test
		@DisplayName("다른 회원이 쓰고 있으면 false다. 앞뒤 공백은 떼고 본다")
		void taken() {
			assertThat(check(" 별지기 ", null)).hasStatusOk().bodyJson()
				.extractingPath("$.data.validated").isEqualTo(false);
		}

		@Test
		@DisplayName("로그인한 채로 자기 닉네임을 물으면 true다")
		void own() {
			assertThat(check("별지기", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))).hasStatusOk().bodyJson()
				.extractingPath("$.data.validated").isEqualTo(true);
		}

		@Test
		@DisplayName("규칙에 어긋나면 400이다")
		void invalid() {
			assertThat(check("별", null)).hasStatus(400).bodyJson()
				.extractingPath("$.message")
				.isEqualTo("닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다.");
		}

		private MvcTestResult check(String nickname, String authorization) {
			var request = mvc.get().uri("/api/v1/members/exists/nickname").param("nickname", nickname);
			if (authorization != null) {
				request = request.header("Authorization", authorization);
			}
			return request.exchange();
		}
	}

	@Nested
	@DisplayName("POST /api/v1/admin/members/{memberId}/nickname/reset")
	class AdminReset {

		@Test
		@DisplayName("관리자는 회원의 닉네임을 별지기와 숫자로 된 기본 닉네임으로 되돌린다")
		void resets() {
			Member admin = memberRepository.save(Member.builder()
				.email("admin@example.com").password("password").nickname("운영자").role(Role.ADMIN)
				.authProvider(AuthProvider.LOCAL).build());

			MvcTestResult result = reset(admin, member.getId());

			assertThat(result).hasStatusOk().bodyJson()
				.extractingPath("$.data.nickname").asString().matches("별지기\\d{4}");
			assertThat(memberRepository.findById(member.getId())).get()
				.extracting(Member::getNickname).asString().matches("별지기\\d{4}");
		}

		@Test
		@DisplayName("관리자가 아니면 403이고 닉네임은 그대로다. 토큰의 역할이 아니라 회원의 역할을 본다")
		void notAdmin() {
			Member other = memberRepository.save(Member.local("other@example.com", "password", "은하수"));

			assertThat(reset(other, member.getId())).hasStatus(403).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 403,
				  "message": "관리자만 할 수 있습니다."
				}
				""");
			assertThat(memberRepository.findById(member.getId())).get()
				.extracting(Member::getNickname).isEqualTo("별지기");
		}

		@Test
		@DisplayName("토큰이 없으면 401이다")
		void withoutToken() {
			assertThat(mvc.post().uri("/api/v1/admin/members/{id}/nickname/reset", member.getId()).exchange())
				.hasStatus(401);
		}

		private MvcTestResult reset(Member actor, Long targetId) {
			return mvc.post().uri("/api/v1/admin/members/{id}/nickname/reset", targetId)
				.header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(actor.getId()))
				.exchange();
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
