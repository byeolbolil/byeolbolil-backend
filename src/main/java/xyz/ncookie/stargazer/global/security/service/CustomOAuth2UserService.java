package xyz.ncookie.stargazer.global.security.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.application.MemberApplicationService;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.global.security.oauth.WithdrawalAwareAuthorizationRequestRepository;
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

	/** 탈퇴를 확인하지 못했을 때 실패 처리기에 넘기는 오류 코드 */
	public static final String WITHDRAWAL_REJECTED = "withdrawal_rejected";

	private final MemberRepository memberRepository;
	private final MemberDomainService memberDomainService;
	private final MemberApplicationService memberApplicationService;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

		OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(provider, oAuth2User.getAttributes());

		String withdrawalTicket = WithdrawalAwareAuthorizationRequestRepository.withdrawalTicket(currentRequest());
		if (withdrawalTicket != null) {
			return withdraw(withdrawalTicket, provider, userInfo, userRequest, oAuth2User);
		}

		Registration registration = findOrRegister(provider, userInfo);
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

	/**
	 * 앱에서 받은 탈퇴 표를 들고 제공자 로그인을 마친 경우다. 표의 회원과 방금 로그인한 계정이 같을 때만 탈퇴시킨다.
	 * 방금 받은 액세스 토큰으로 제공자 연결을 끊는다. 가입하지 않은 계정이면 새로 가입시키지 않는다.
	 */
	private OAuth2User withdraw(String ticket, AuthProvider provider, OAuth2UserInfo userInfo,
		OAuth2UserRequest userRequest, OAuth2User oAuth2User) {

		try {
			Long memberId = memberApplicationService.withdrawSocial(ticket, provider, userInfo.getProviderId(),
				userRequest.getAccessToken().getTokenValue());
			return MemberPrincipal.withdrawn(memberId, oAuth2User.getAttributes());
		} catch (MemberException e) {
			throw new OAuth2AuthenticationException(new OAuth2Error(WITHDRAWAL_REJECTED), e.getMessage());
		}
	}

	private static HttpServletRequest currentRequest() {
		return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
			? attributes.getRequest()
			: null;
	}

	record Registration(Member member, boolean created) {
	}
}
