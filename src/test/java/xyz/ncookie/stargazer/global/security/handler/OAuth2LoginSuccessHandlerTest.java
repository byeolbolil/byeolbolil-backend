package xyz.ncookie.stargazer.global.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import xyz.ncookie.stargazer.domain.member.entity.Role;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 소셜 로그인에 성공하면 웹의 콜백 화면으로 보낸다. 방금 가입한 사람이면 첫 닉네임 화면을 띄우라고 welcome=1을 붙인다.
 */
class OAuth2LoginSuccessHandlerTest extends IntegrationTestSupport {

	@Autowired
	private OAuth2LoginSuccessHandler handler;

	@Test
	@DisplayName("방금 가입한 사람은 welcome=1을 붙여 보내고 리프레시 토큰 쿠키를 심는다")
	void newMember() throws Exception {
		MockHttpServletResponse response = succeed(true);

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/oauth/callback?welcome=1");
		assertThat(response.getHeader("Set-Cookie")).startsWith("refresh_token=");
	}

	@Test
	@DisplayName("이미 가입한 사람은 콜백 주소만으로 보낸다")
	void returningMember() throws Exception {
		assertThat(succeed(false).getRedirectedUrl()).isEqualTo("http://localhost:3000/oauth/callback");
	}

	private MockHttpServletResponse succeed(boolean newMember) throws Exception {
		MemberPrincipal principal = new MemberPrincipal(1L, Role.USER, Map.of(), newMember);
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, new TestingAuthenticationToken(principal, null));
		return response;
	}
}
