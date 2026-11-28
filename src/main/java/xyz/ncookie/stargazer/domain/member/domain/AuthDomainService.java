package xyz.ncookie.stargazer.domain.member.domain;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthDomainService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 이메일이 없을 때와 비밀번호가 틀렸을 때를 같은 오류로 답한다.
	 * 둘을 나누면 로그인 화면에서 응답만 보고 가입된 이메일인지 알 수 있다.
	 * 소셜 회원은 같은 이메일이어도 찾지 않는다.
	 */
	public Member authenticate(String email, String rawPassword) {

		Member member = memberRepository.findByEmailAndAuthProvider(email, AuthProvider.LOCAL)
			.orElseThrow(() -> new MemberException(MemberErrorCode.LOGIN_FAILED));

		if (member.getPassword() == null || !passwordEncoder.matches(rawPassword, member.getPassword())) {
			throw new MemberException(MemberErrorCode.LOGIN_FAILED);
		}
		return member;
	}

	public String encodePassword(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}
}
