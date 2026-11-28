package xyz.ncookie.stargazer.global.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.entity.Role;

/**
 * 자체 로그인과 OAuth 로그인 모두 동일한 Principal을 사용
 */
@Getter
@RequiredArgsConstructor
public class MemberPrincipal implements UserDetails, OAuth2User {

	private final Long memberId;
	private final Role role;
	private final Map<String, Object> attributes;
	/** 소셜 로그인으로 방금 가입했는지. 프론트가 첫 닉네임 화면을 띄우는 데 쓴다 */
	private final boolean newMember;
	/** 로그인이 아니라 탈퇴였는지. 성공 처리기가 토큰을 내주지 않고 프론트로 돌려보낸다 */
	private final boolean withdrawn;

	public MemberPrincipal(Long memberId, Role role) {
		this(memberId, role, Collections.emptyMap(), false, false);
	}

	public MemberPrincipal(Long memberId, Role role, Map<String, Object> attributes, boolean newMember) {
		this(memberId, role, attributes, newMember, false);
	}

	/** 소셜 로그인을 거쳐 방금 탈퇴한 회원 */
	public static MemberPrincipal withdrawn(Long memberId, Map<String, Object> attributes) {
		return new MemberPrincipal(memberId, Role.USER, attributes, false, true);
	}

	// OAuth2
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	// 공통
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.getKey()));
	}

	@Override
	public @Nullable String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return String.valueOf(memberId);
	}

	@Override
	public String getName() {
		return String.valueOf(memberId);
	}

	// 기타는 true 처리
	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }
	@Override public boolean isEnabled() { return true; }
}
