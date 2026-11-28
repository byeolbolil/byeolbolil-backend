package xyz.ncookie.stargazer.global.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.application.MemberApplicationService;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.oauth.GoogleOAuth2UserInfo;
import xyz.ncookie.stargazer.global.security.oauth.KakaoOAuth2UserInfo;
import xyz.ncookie.stargazer.global.security.oauth.NaverOAuth2UserInfo;
import xyz.ncookie.stargazer.global.security.oauth.OAuth2UserInfo;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 소셜 가입의 기본 닉네임과, 닉네임을 바꾼 뒤 다시 로그인해도 바꾼 닉네임이 남는지 고정한다.
 * 회원은 제공자와 제공자 ID로 찾고, 이메일이 같아도 다른 제공자의 회원과 합치지 않는다.
 * 제공자의 사용자 정보 호출은 건너뛰고, 그 결과로 회원을 찾거나 가입시키는 부분만 본다.
 */
class CustomOAuth2UserServiceTest extends IntegrationTestSupport {

	private static final OAuth2UserInfo GOOGLE_USER = new GoogleOAuth2UserInfo(Map.of(
		"sub", "google-123",
		"email", "gildong@example.com",
		"name", "홍길동"
	));

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private MemberDomainService memberDomainService;

	@Autowired
	private MemberApplicationService memberApplicationService;

	private CustomOAuth2UserService customOAuth2UserService;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		// 빈은 트랜잭션 프록시라 패키지 안 메서드를 부르면 대상 객체에 닿지 않을 수 있다. 저장소만 같은 것을 쓴다.
		customOAuth2UserService = new CustomOAuth2UserService(memberRepository, memberDomainService, memberApplicationService);
	}

	@Test
	@DisplayName("처음 들어오면 제공자가 준 이름(대개 실명) 대신 별지기와 숫자 네 자리로 된 기본 닉네임으로 가입하고, 가입했다고 알린다")
	void firstLoginGetsDefaultNickname() {
		CustomOAuth2UserService.Registration registration =
			customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER);

		assertThat(registration.created()).isTrue();
		assertThat(registration.member().getNickname()).matches("별지기\\d{4}").isNotEqualTo("홍길동");
	}

	@Test
	@DisplayName("기본 닉네임은 이미 있는 닉네임과 겹치지 않는다")
	void defaultNicknamesDoNotCollide() {
		for (int i = 0; i < 30; i++) {
			customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, new GoogleOAuth2UserInfo(Map.of(
				"sub", "google-" + i, "email", i + "@example.com", "name", "홍길동")));
		}

		assertThat(memberRepository.findAll()).extracting(Member::getNickname).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("닉네임을 바꾼 뒤 다시 로그인해도 제공자의 이름으로 돌아가지 않는다")
	void reloginKeepsChangedNickname() {
		Member member = customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER).member();
		member.changeNickname("밤하늘 산책");
		memberRepository.save(member);

		CustomOAuth2UserService.Registration again = customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER);

		assertThat(again.created()).isFalse();
		assertThat(again.member().getId()).isEqualTo(member.getId());
		assertThat(memberRepository.findById(member.getId())).get()
			.extracting(Member::getNickname).isEqualTo("밤하늘 산책");
		assertThat(memberRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("다른 제공자에 같은 이메일로 가입한 사람이 있어도 새 회원으로 가입한다")
	void sameEmailOtherProvider() {
		Member google = customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER).member();

		CustomOAuth2UserService.Registration naver = customOAuth2UserService.findOrRegister(AuthProvider.NAVER,
			new NaverOAuth2UserInfo(Map.of("response", Map.of("id", "naver-1", "email", "gildong@example.com"))));

		assertThat(naver.created()).isTrue();
		assertThat(naver.member().getId()).isNotEqualTo(google.getId());
		assertThat(memberRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("카카오가 이메일을 주지 않아도 가입한다")
	void kakaoWithoutEmail() {
		CustomOAuth2UserService.Registration registration = customOAuth2UserService.findOrRegister(AuthProvider.KAKAO,
			new KakaoOAuth2UserInfo(Map.of("id", 1234L, "kakao_account", Map.of())));

		assertThat(registration.created()).isTrue();
		assertThat(memberRepository.findById(registration.member().getId())).get()
			.extracting(Member::getEmail).isNull();
	}
}
