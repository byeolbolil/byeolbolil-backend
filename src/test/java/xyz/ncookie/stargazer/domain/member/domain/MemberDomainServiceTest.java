package xyz.ncookie.stargazer.domain.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.text.Normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.entity.Role;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

/**
 * 닉네임 규칙의 경계를 고정한다. 규칙은 docs/decisions.md 2026-11-27 "닉네임 수정"에 있다.
 */
class MemberDomainServiceTest {

	@Nested
	@DisplayName("닉네임 규칙")
	class NicknameRule {

		@ParameterizedTest
		@ValueSource(strings = {"별지", "별지기", "stargazer", "star_gazer", "별지기2026", "Gildong Hong", "ㅋㅋ", "__"})
		@DisplayName("한글, 영문, 숫자, 밑줄과 낱말 사이의 공백 하나는 받는다")
		void accepted(String nickname) {
			assertThat(MemberDomainService.normalizeNickname(nickname)).isEqualTo(nickname);
		}

		@Test
		@DisplayName("1자는 받지 않고 2자부터 받는다")
		void minLength() {
			assertRejected("별");
			assertThat(MemberDomainService.normalizeNickname("별지")).isEqualTo("별지");
		}

		@Test
		@DisplayName("16자까지 받고 17자는 받지 않는다")
		void maxLength() {
			String sixteen = "별".repeat(16);
			assertThat(MemberDomainService.normalizeNickname(sixteen)).isEqualTo(sixteen);
			assertRejected("별".repeat(17));
		}

		@Test
		@DisplayName("앞뒤 공백은 떼고 저장하며, 길이는 뗀 뒤에 센다")
		void stripsBeforeCounting() {
			assertThat(MemberDomainService.normalizeNickname("  별지기  ")).isEqualTo("별지기");
			assertThat(MemberDomainService.normalizeNickname(" " + "별".repeat(16) + " ")).isEqualTo("별".repeat(16));
			assertRejected(" 별 ");
		}

		@ParameterizedTest
		@NullSource
		@ValueSource(strings = {"", "   ", "\t\t"})
		@DisplayName("비었거나 공백만 있으면 받지 않는다")
		void blank(String nickname) {
			assertRejected(nickname);
		}

		@ParameterizedTest
		@ValueSource(strings = {"별  지기", "별\t지기", "별\n지기", "별　지기"})
		@DisplayName("연속 공백, 탭, 줄바꿈, 전각 공백은 받지 않는다")
		void innerWhitespace(String nickname) {
			assertRejected(nickname);
		}

		@ParameterizedTest
		@ValueSource(strings = {"별지기⭐", "🌙달", "별-지기", "별.지기", "<script>", "ほし", "星星"})
		@DisplayName("이모지, 기호, 다른 문자는 받지 않는다")
		void otherCharacters(String nickname) {
			assertRejected(nickname);
		}

		@Test
		@DisplayName("자모로 풀어 보낸 한글(NFD)은 음절로 모아 받는다")
		void decomposedHangul() {
			String decomposed = Normalizer.normalize("별지기", Normalizer.Form.NFD);
			assertThat(decomposed).hasSizeGreaterThan(3);

			assertThat(MemberDomainService.normalizeNickname(decomposed)).isEqualTo("별지기");
		}

		private void assertRejected(String nickname) {
			assertThatThrownBy(() -> MemberDomainService.normalizeNickname(nickname))
				.isInstanceOf(MemberException.class)
				.extracting(e -> ((MemberException)e).getErrorCode())
				.isEqualTo(MemberErrorCode.INVALID_NICKNAME);
		}
	}

	@Nested
	@DisplayName("겹침과 기본 닉네임")
	class Uniqueness {

		private final MemberRepository repository = mock(MemberRepository.class);
		private final MemberDomainService service = new MemberDomainService(repository);

		@Test
		@DisplayName("가입할 때 다른 회원이 쓰는 닉네임이면 409다")
		void takenOnRegister() {
			when(repository.existsByNickname("은하수")).thenReturn(true);

			assertThatThrownBy(() -> service.validateNewNickname(" 은하수 ", null))
				.isInstanceOf(MemberException.class)
				.extracting(e -> ((MemberException)e).getErrorCode())
				.isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED);
		}

		@Test
		@DisplayName("바꿀 때는 자기 자신을 빼고 겹치는지 본다")
		void changeExcludesSelf() {
			when(repository.existsByNicknameAndIdNot("Stargazer", 7L)).thenReturn(false);

			assertThat(service.validateNewNickname("Stargazer", 7L)).isEqualTo("Stargazer");
			verify(repository, never()).existsByNickname(anyString());
		}

		@Test
		@DisplayName("규칙 검사가 먼저다. 어긋나면 저장소에 묻지 않고 400이다")
		void ruleBeforeLookup() {
			assertThatThrownBy(() -> service.validateNewNickname("별", null))
				.extracting(e -> ((MemberException)e).getErrorCode())
				.isEqualTo(MemberErrorCode.INVALID_NICKNAME);
			verifyNoInteractions(repository);
		}

		@Test
		@DisplayName("기본 닉네임은 별지기와 숫자 네 자리이고, 규칙에 맞는다")
		void defaultNickname() {
			String nickname = service.generateDefaultNickname();

			assertThat(nickname).matches("별지기\\d{4}");
			assertThat(MemberDomainService.normalizeNickname(nickname)).isEqualTo(nickname);
		}

		@Test
		@DisplayName("네 자리가 다섯 번 겹치면 여섯 자리로 늘린다")
		void longerWhenTaken() {
			when(repository.existsByNickname(anyString())).thenReturn(true, true, true, true, true, false);

			assertThat(service.generateDefaultNickname()).matches("별지기\\d{6}");
		}

		@Test
		@DisplayName("관리자가 아니면 403이다")
		void adminOnly() {
			Member user = Member.local("a@example.com", "pw", "별지기");
			Member admin = Member.builder().email("b@example.com").nickname("운영자").role(Role.ADMIN).build();

			assertThatThrownBy(() -> service.validateAdmin(user))
				.extracting(e -> ((MemberException)e).getErrorCode())
				.isEqualTo(MemberErrorCode.ADMIN_ONLY);
			service.validateAdmin(admin);
		}
	}

	@Test
	@DisplayName("바꿀 때는 앞뒤 공백을 뗀 값을 회원에 넣는다")
	void changeNickname() {
		MemberDomainService service = new MemberDomainService(mock(MemberRepository.class));
		Member member = Member.oauth("a@example.com", "홍길동", null, "1");

		service.changeNickname(member, " 별지기 ");

		assertThat(member.getNickname()).isEqualTo("별지기");
	}

	@Test
	@DisplayName("규칙에 어긋나면 회원의 닉네임은 그대로다")
	void rejectedKeepsOldNickname() {
		MemberDomainService service = new MemberDomainService(mock(MemberRepository.class));
		Member member = Member.oauth("a@example.com", "홍길동😀", null, "1");

		assertThatThrownBy(() -> service.changeNickname(member, "별"))
			.isInstanceOf(MemberException.class);
		assertThat(member.getNickname()).isEqualTo("홍길동😀");
	}
}
