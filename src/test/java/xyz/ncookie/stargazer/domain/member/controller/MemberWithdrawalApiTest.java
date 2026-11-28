package xyz.ncookie.stargazer.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.application.MemberApplicationService;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 회원 탈퇴. 이메일 회원은 비밀번호로 바로 탈퇴하고, 소셜 회원은 탈퇴 표를 받아 제공자 로그인을 거친다.
 * 제공자 로그인 자체는 건너뛰고, 콜백에서 부르는 {@code withdrawSocial}부터 본다.
 */
class MemberWithdrawalApiTest extends IntegrationTestSupport {

	private static final String EMAIL = "stargazer@example.com";
	private static final String PASSWORD = "starry-night1!";

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private RefreshTokenRedisRepository refreshTokenRedisRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MemberApplicationService memberApplicationService;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("DELETE /api/v1/members/me")
	class LocalWithdrawal {

		private Member member;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(Member.local(EMAIL, passwordEncoder.encode(PASSWORD), "별지기"));
			bookmarkRepository.save(bookmarkOf(member));
		}

		@Test
		@DisplayName("비밀번호가 맞으면 회원과 북마크를 지우고 리프레시 토큰 쿠키를 비운다. data는 없다")
		void withdraws() {
			String refreshToken = refreshTokenOf(member);

			MvcTestResult result = withdraw(member, refreshToken, PASSWORD);

			assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
				""");
			assertThat(result.getResponse().getHeader("Set-Cookie")).startsWith("refresh_token=;").contains("Max-Age=0");
			assertThat(memberRepository.findById(member.getId())).isEmpty();
			assertThat(bookmarkRepository.count()).isZero();
			assertThat(reissue(refreshToken)).hasStatus(401);
		}

		@Test
		@DisplayName("다른 기기에 남은 리프레시 토큰은 다음 재발급에서 401이다")
		void otherDeviceIsLoggedOut() {
			String otherDevice = refreshTokenOf(member);

			withdraw(member, null, PASSWORD);

			assertThat(reissue(otherDevice)).hasStatus(401).bodyJson()
				.extractingPath("$.message").isEqualTo("유효하지 않은 Refresh Token 입니다.");
		}

		@Test
		@DisplayName("만료되지 않은 액세스 토큰으로 내 정보를 부르면 404다")
		void accessTokenAfterWithdrawal() {
			withdraw(member, null, PASSWORD);

			assertThat(mvc.get().uri("/api/v1/members/me").header("Authorization", bearer(member)).exchange())
				.hasStatus(404).bodyJson().extractingPath("$.message").isEqualTo("사용자를 찾을 수 없습니다");
		}

		@Test
		@DisplayName("비밀번호가 틀리거나 없으면 400이고 아무것도 지우지 않는다. 401이 아니어서 클라이언트가 재발급하지 않는다")
		void wrongPassword() {
			assertThat(withdraw(member, null, "wrong-password1!")).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{ "success": false, "status": 400, "message": "비밀번호가 맞지 않습니다." }
				""");
			assertThat(mvc.delete().uri("/api/v1/members/me").header("Authorization", bearer(member)).exchange())
				.hasStatus(400);
			assertThat(memberRepository.findById(member.getId())).isPresent();
			assertThat(bookmarkRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("소셜 회원이 부르면 400이다")
		void socialMember() {
			Member social = memberRepository.save(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));

			assertThat(withdraw(social, null, PASSWORD)).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("소셜 로그인 회원은 가입한 서비스에 다시 로그인해서 탈퇴합니다.");
			assertThat(memberRepository.findById(social.getId())).isPresent();
		}

		@Test
		@DisplayName("토큰이 없으면 401이다")
		void withoutToken() {
			assertThat(mvc.delete().uri("/api/v1/members/me").exchange()).hasStatus(401);
		}
	}

	@Nested
	@DisplayName("POST /api/v1/members/me/withdrawal-ticket")
	class Ticket {

		@Test
		@DisplayName("소셜 회원에게 제공자 로그인 주소에 탈퇴 표를 붙여 준다")
		void socialMember() {
			Member kakao = memberRepository.save(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));

			assertThat(ticket(kakao)).hasStatusOk().bodyJson()
				.extractingPath("$.data.authorizationUrl").asString()
				.matches("/oauth2/authorization/kakao\\?withdraw=[0-9a-f-]{36}");
		}

		@Test
		@DisplayName("이메일 회원이 부르면 400이다")
		void localMember() {
			Member local = memberRepository.save(Member.local(EMAIL, passwordEncoder.encode(PASSWORD), "별지기"));

			assertThat(ticket(local)).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("이메일로 가입한 회원은 비밀번호를 넣어 탈퇴합니다.");
		}

		@Test
		@DisplayName("제공자 로그인 주소로 가면 제공자의 인가 화면으로 보낸다")
		void authorizationRedirect() throws Exception {
			Member kakao = memberRepository.save(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));
			String url = JsonPath.read(ticket(kakao).getResponse().getContentAsString(), "$.data.authorizationUrl");

			MvcTestResult result = mvc.get().uri(url).exchange();

			assertThat(result).hasStatus(302);
			assertThat(result.getResponse().getHeader("Location")).startsWith("https://kauth.kakao.com/oauth/authorize");
		}

		private MvcTestResult ticket(Member who) {
			return mvc.post().uri("/api/v1/members/me/withdrawal-ticket").header("Authorization", bearer(who)).exchange();
		}
	}

	@Nested
	@DisplayName("소셜 회원 탈퇴 (제공자 로그인 뒤)")
	class SocialWithdrawal {

		@Test
		@DisplayName("표의 회원과 로그인한 계정이 같으면 카카오 연결을 끊고 회원과 북마크를 지운다")
		void kakao() {
			Member kakao = memberRepository.save(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));
			bookmarkRepository.save(bookmarkOf(kakao));
			externalApi.kakaoUnlinkAccepts("kakao-access");

			Long withdrawn = memberApplicationService.withdrawSocial(ticketOf(kakao), AuthProvider.KAKAO, "kakao-1",
				"kakao-access");

			assertThat(withdrawn).isEqualTo(kakao.getId());
			externalApi.verifyAllCalled();
			assertThat(memberRepository.count()).isZero();
			assertThat(bookmarkRepository.count()).isZero();
		}

		@Test
		@DisplayName("네이버는 앱 키와 회원의 토큰으로 연동을 해제한다")
		void naver() {
			Member naver = memberRepository.save(Member.oauth("a@example.com", "네이버", AuthProvider.NAVER, "naver-1"));
			externalApi.naverUnlinkAccepts("naver-access");

			memberApplicationService.withdrawSocial(ticketOf(naver), AuthProvider.NAVER, "naver-1", "naver-access");

			externalApi.verifyAllCalled();
			assertThat(memberRepository.count()).isZero();
		}

		@Test
		@DisplayName("구글은 토큰을 취소한다")
		void google() {
			Member google = memberRepository.save(Member.oauth("a@example.com", "구글", AuthProvider.GOOGLE, "google-1"));
			externalApi.googleRevokeAccepts("google-access");

			memberApplicationService.withdrawSocial(ticketOf(google), AuthProvider.GOOGLE, "google-1", "google-access");

			externalApi.verifyAllCalled();
			assertThat(memberRepository.count()).isZero();
		}

		@Test
		@DisplayName("제공자 연결 끊기에 실패해도 탈퇴는 된다")
		void unlinkFailureDoesNotBlock() {
			Member kakao = memberRepository.save(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));
			Member naver = memberRepository.save(Member.oauth(null, "네이버", AuthProvider.NAVER, "naver-1"));
			externalApi.kakaoUnlinkFails();
			externalApi.naverUnlinkRejects();

			memberApplicationService.withdrawSocial(ticketOf(kakao), AuthProvider.KAKAO, "kakao-1", "kakao-access");
			memberApplicationService.withdrawSocial(ticketOf(naver), AuthProvider.NAVER, "naver-1", "naver-access");

			assertThat(memberRepository.count()).isZero();
		}

		@Test
		@DisplayName("표의 회원과 다른 계정으로 로그인했으면 거절하고 아무도 지우지 않는다. 표는 다시 쓸 수 없다")
		void otherAccount() {
			Member mine = memberRepository.save(Member.oauth(null, "내 계정", AuthProvider.KAKAO, "kakao-1"));
			memberRepository.save(Member.oauth(null, "다른 계정", AuthProvider.KAKAO, "kakao-2"));
			String ticket = ticketOf(mine);

			assertRejected(() -> memberApplicationService.withdrawSocial(ticket, AuthProvider.KAKAO, "kakao-2", "token"));
			assertRejected(() -> memberApplicationService.withdrawSocial(ticket, AuthProvider.KAKAO, "kakao-1", "token"));
			assertThat(memberRepository.count()).isEqualTo(2);
		}

		@Test
		@DisplayName("가입하지 않은 계정이나 없는 표면 거절하고 새로 가입시키지도 않는다")
		void unknownAccountOrTicket() {
			Member mine = memberRepository.save(Member.oauth(null, "내 계정", AuthProvider.KAKAO, "kakao-1"));

			assertRejected(() -> memberApplicationService.withdrawSocial(ticketOf(mine), AuthProvider.NAVER, "naver-9", "t"));
			assertRejected(() -> memberApplicationService.withdrawSocial("no-such-ticket", AuthProvider.KAKAO, "kakao-1", "t"));
			assertThat(memberRepository.count()).isEqualTo(1);
		}

		private void assertRejected(Runnable call) {
			MemberException e = catchThrowableOfType(MemberException.class, call::run);
			assertThat(e).isNotNull();
			assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.WITHDRAWAL_REJECTED);
		}

		private String ticketOf(Member who) {
			String url = memberApplicationService.issueWithdrawalTicket(who.getId()).authorizationUrl();
			return url.substring(url.indexOf("withdraw=") + "withdraw=".length());
		}
	}

	private MvcTestResult withdraw(Member who, String refreshToken, String password) {
		var request = mvc.delete().uri("/api/v1/members/me")
			.header("Authorization", bearer(who))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"password\": \"" + password + "\" }");
		if (refreshToken != null) {
			request = request.cookie(new Cookie("refresh_token", refreshToken));
		}
		return request.exchange();
	}

	private MvcTestResult reissue(String refreshToken) {
		return mvc.post().uri("/api/v1/auth/reissue").cookie(new Cookie("refresh_token", refreshToken)).exchange();
	}

	private String refreshTokenOf(Member who) {
		String token = jwtTokenProvider.createRefreshToken(who.getId());
		refreshTokenRedisRepository.save(token, who.getId(), JwtTokenProvider.REFRESH_EXPIRE_MS);
		return token;
	}

	private String bearer(Member who) {
		return "Bearer " + jwtTokenProvider.createAccessToken(who.getId());
	}

	private static Bookmark bookmarkOf(Member owner) {
		return new Bookmark(owner, BookmarkType.CUSTOM, null, "뒷산", 37.5, 127.0, "서울", null);
	}
}
