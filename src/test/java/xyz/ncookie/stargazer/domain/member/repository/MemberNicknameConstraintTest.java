package xyz.ncookie.stargazer.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 앱의 검사를 지나 같은 순간에 들어온 두 저장은 DB의 유일 제약이 막는다. 운영과 같은 콜레이션이라 대소문자도 가리지 않는다.
 * 소셜 회원은 제공자와 제공자 ID가 겹치지 않고, 이메일은 겹쳐도 된다.
 */
class MemberNicknameConstraintTest extends IntegrationTestSupport {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private MemberDomainService memberDomainService;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		memberRepository.saveAndFlush(Member.local("a@example.com", "pw", "Stargazer"));
	}

	@Test
	@DisplayName("대소문자만 다른 닉네임도 DB가 막고, 그 실패는 409로 바뀐다")
	void caseInsensitiveUnique() {
		DataIntegrityViolationException e = catchThrowableOfType(DataIntegrityViolationException.class,
			() -> memberRepository.saveAndFlush(Member.local("b@example.com", "pw", "stargazer")));

		assertThat(e).isNotNull();
		assertThat(memberDomainService.nicknameConflictOr(e))
			.isInstanceOf(MemberException.class)
			.extracting(ex -> ((MemberException)ex).getErrorCode())
			.isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED);
	}

	@Test
	@DisplayName("다른 제약에 걸린 것은 닉네임 충돌로 바꾸지 않는다")
	void otherConstraint() {
		memberRepository.saveAndFlush(Member.oauth(null, "카카오1", AuthProvider.KAKAO, "kakao-1"));

		DataIntegrityViolationException e = catchThrowableOfType(DataIntegrityViolationException.class,
			() -> memberRepository.saveAndFlush(Member.oauth(null, "카카오2", AuthProvider.KAKAO, "kakao-1")));

		assertThat(e).isNotNull();
		assertThat(memberDomainService.nicknameConflictOr(e)).isSameAs(e);
	}

	@Test
	@DisplayName("제공자 ID가 같아도 제공자가 다르면 다른 회원이다")
	void providerIdPerProvider() {
		memberRepository.saveAndFlush(Member.oauth(null, "카카오", AuthProvider.KAKAO, "1234"));
		memberRepository.saveAndFlush(Member.oauth(null, "네이버", AuthProvider.NAVER, "1234"));

		assertThat(memberRepository.count()).isEqualTo(3);
	}

	@Test
	@DisplayName("이메일은 겹쳐도 되고 비어 있어도 된다")
	void emailMayRepeat() {
		memberRepository.saveAndFlush(Member.oauth("a@example.com", "구글", AuthProvider.GOOGLE, "google-1"));
		memberRepository.saveAndFlush(Member.oauth("a@example.com", "네이버", AuthProvider.NAVER, "naver-1"));
		memberRepository.saveAndFlush(Member.oauth(null, "카카오", AuthProvider.KAKAO, "kakao-1"));

		assertThat(memberRepository.count()).isEqualTo(4);
	}

	@Test
	@DisplayName("닉네임이 없는 회원은 여럿이어도 된다")
	void nullsAllowed() {
		memberRepository.saveAndFlush(Member.local("c@example.com", "pw", null));
		memberRepository.saveAndFlush(Member.local("d@example.com", "pw", null));

		assertThat(memberRepository.count()).isEqualTo(3);
	}
}
