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
@Table(name = "member", uniqueConstraints = @UniqueConstraint(name = Member.NICKNAME_UNIQUE, columnNames = "nickname"))
public class Member {

	public static final String NICKNAME_UNIQUE = "uk_member_nickname";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;				// 소셜 이메일 or 자체 가입 이메일

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
