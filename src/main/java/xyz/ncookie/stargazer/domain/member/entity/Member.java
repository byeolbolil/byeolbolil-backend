package xyz.ncookie.stargazer.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 닉네임은 겹치지 않는다. 앱이 저장 전에 검사하고, 같은 순간의 두 저장은 이 제약이 막는다.
// 비교는 컬럼의 콜레이션(utf8mb4_unicode_ci)을 따라 대소문자를 가리지 않는다(docs/decisions.md 2026-11-28)
// 소셜 회원은 제공자와 제공자 ID로 가리킨다. 처음 로그인이 두 번 겹쳐도 회원은 하나만 생긴다.
// 자체 회원은 제공자 ID가 없고, MySQL은 NULL끼리는 겹친다고 보지 않으므로 이 제약에 걸리지 않는다.
@Table(name = "member", uniqueConstraints = {
	@UniqueConstraint(name = Member.NICKNAME_UNIQUE, columnNames = "nickname"),
	@UniqueConstraint(name = Member.PROVIDER_UNIQUE, columnNames = {"authProvider", "providerId"})
})
public class Member {

	public static final String NICKNAME_UNIQUE = "uk_member_nickname";
	public static final String PROVIDER_UNIQUE = "uk_member_provider";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 자체 회원은 로그인 아이디라 자체 회원 사이에서 겹치지 않는다(앱이 가입할 때 검사한다).
	// 소셜 회원은 연락처일 뿐이라 다른 제공자의 회원과 겹칠 수 있고, 카카오처럼 주지 않으면 비어 있다
	// (docs/decisions.md 2026-11-28 "소셜 계정은 이메일이 같아도 합치지 않는다")
	private String email;

	private String password;	// 자체 회원만 사용

	private String nickname;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Enumerated(EnumType.STRING)
	private AuthProvider authProvider;

	private String providerId;	// oauth2 회원만 사용

	@Builder
	public Member(String email, String password, String nickname, Role role, AuthProvider authProvider, String providerId) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.role = role;
		this.authProvider = authProvider;
		this.providerId = providerId;
	}

	public static Member oauth(String email, String nickname, AuthProvider authProvider, String providerId) {
		return Member.builder()
			.email(email)
			.nickname(nickname)
			.role(Role.USER)
			.authProvider(authProvider)
			.providerId(providerId)
			.build();
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	/** 인코딩한 비밀번호를 받는다 */
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	public static Member local(String email, String password, String nickname) {
		return Member.builder()
			.email(email)
			.password(password)
			.nickname(nickname)
			.role(Role.USER)
			.authProvider(AuthProvider.LOCAL)
			.build();
	}
}
