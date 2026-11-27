package xyz.ncookie.stargazer.global.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.oauth.GoogleOAuth2UserInfo;
import xyz.ncookie.stargazer.global.security.oauth.OAuth2UserInfo;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 소셜 로그인으로 들어온 회원이 닉네임을 바꾼 뒤 다시 로그인해도 바꾼 닉네임이 남는지 고정한다.
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

	private CustomOAuth2UserService customOAuth2UserService;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		// 빈은 트랜잭션 프록시라 패키지 안 메서드를 부르면 대상 객체에 닿지 않을 수 있다. 저장소만 같은 것을 쓴다.
		customOAuth2UserService = new CustomOAuth2UserService(memberRepository);
	}

	@Test
	@DisplayName("처음 들어오면 제공자가 준 이름이 닉네임이 된다")
	void firstLoginUsesProviderName() {
		Member member = customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER);

		assertThat(member.getNickname()).isEqualTo("홍길동");
	}

	@Test
	@DisplayName("닉네임을 바꾼 뒤 다시 로그인해도 제공자의 이름으로 돌아가지 않는다")
	void reloginKeepsChangedNickname() {
		Member member = customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER);
		member.changeNickname("별지기");
		memberRepository.save(member);

		Member again = customOAuth2UserService.findOrRegister(AuthProvider.GOOGLE, GOOGLE_USER);

		assertThat(again.getId()).isEqualTo(member.getId());
		assertThat(memberRepository.findById(member.getId())).get()
			.extracting(Member::getNickname).isEqualTo("별지기");
		assertThat(memberRepository.count()).isEqualTo(1);
	}
}
