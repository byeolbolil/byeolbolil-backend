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

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

		Member member = findOrRegister(provider, OAuth2UserInfoFactory.of(provider, oAuth2User.getAttributes()));

		// SecurityContext에 저장할 객체 반환
		return new MemberPrincipal(member.getId(), member.getRole(), oAuth2User.getAttributes());
	}

	/**
	 * 제공자가 준 닉네임은 처음 가입할 때만 쓴다. 이미 가입한 회원은 그대로 돌려주므로
	 * 회원이 바꾼 닉네임이 다시 로그인할 때 제공자의 이름으로 돌아가지 않는다.
	 */
	Member findOrRegister(AuthProvider provider, OAuth2UserInfo userInfo) {

		return memberRepository
			.findByAuthProviderAndProviderId(provider, userInfo.getProviderId())
			.orElseGet(() ->
				memberRepository.save(
					Member.oauth(
						userInfo.getEmail(),
						userInfo.getNickname(),
						provider,
						userInfo.getProviderId()
					)
				));
	}
}
