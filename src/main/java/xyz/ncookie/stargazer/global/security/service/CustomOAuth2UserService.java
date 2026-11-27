package xyz.ncookie.stargazer.global.security.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.global.security.oauth.OAuth2UserInfoFactory;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;
import xyz.ncookie.stargazer.global.security.oauth.OAuth2UserInfo;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final MemberRepository memberRepository;
	private final MemberDomainService memberDomainService;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

		Registration registration = findOrRegister(provider, OAuth2UserInfoFactory.of(provider, oAuth2User.getAttributes()));
		Member member = registration.member();

		// SecurityContext에 저장할 객체 반환
		return new MemberPrincipal(member.getId(), member.getRole(), oAuth2User.getAttributes(), registration.created());
	}

	/**
	 * 처음 들어온 회원은 "별지기1234" 꼴의 기본 닉네임으로 가입시킨다. 제공자가 준 이름(구글은 대개 실명)은 닉네임으로 쓰지 않고,
	 * 프론트가 첫 화면에서 닉네임을 정하게 한다(docs/decisions.md 2026-11-28 "닉네임은 겹치지 않고, 소셜 가입은 기본 닉네임으로 시작한다").
	 * 이미 가입한 회원은 그대로 돌려주므로 회원이 바꾼 닉네임이 다시 로그인할 때 덮이지 않는다.
	 */
	Registration findOrRegister(AuthProvider provider, OAuth2UserInfo userInfo) {

		return memberRepository
			.findByAuthProviderAndProviderId(provider, userInfo.getProviderId())
			.map(member -> new Registration(member, false))
			.orElseGet(() -> new Registration(
				memberRepository.save(
					Member.oauth(
						userInfo.getEmail(),
						memberDomainService.generateDefaultNickname(),
						provider,
						userInfo.getProviderId()
					)
				),
				true));
	}

	record Registration(Member member, boolean created) {
	}
}
