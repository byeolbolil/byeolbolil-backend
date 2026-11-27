package xyz.ncookie.stargazer.domain.member.domain;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberDomainService {

	static final int NICKNAME_MIN_LENGTH = 2;
	static final int NICKNAME_MAX_LENGTH = 16;

	// 한글 음절과 자모, 영문, 숫자, 밑줄로 된 낱말을 공백 하나로 잇는다.
	// "Gildong Hong"처럼 제공자가 준 이름이 그대로 들어오도록 공백을 받는다(docs/decisions.md 2026-11-27).
	private static final Pattern NICKNAME = Pattern.compile("[가-힣ㄱ-ㅣA-Za-z0-9_]+( [가-힣ㄱ-ㅣA-Za-z0-9_]+)*");

	private final MemberRepository memberRepository;

	public Member findById(Long memberId) {

		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	public boolean isEmailDuplicated(String email) {
		return memberRepository.existsByEmail(email);
	}

	/**
	 * 규칙은 바꿀 때만 본다. 가입할 때 제공자가 준 닉네임은 규칙에 어긋나도 그대로 둔다.
	 * 다른 회원과 같은 닉네임도 받는다.
	 */
	public void changeNickname(Member member, String nickname) {
		member.changeNickname(normalizeNickname(nickname));
	}

	/**
	 * 앞뒤 공백을 떼고 규칙에 맞는지 본다. 맞으면 저장할 값을, 어긋나면 {@code INVALID_NICKNAME}을 던진다.
	 * iOS 입력기는 한글을 자모로 풀어(NFD) 보낼 때가 있어서 먼저 NFC로 모은다.
	 */
	static String normalizeNickname(String nickname) {

		if (nickname == null) {
			throw new MemberException(MemberErrorCode.INVALID_NICKNAME);
		}

		String normalized = Normalizer.normalize(nickname, Normalizer.Form.NFC).strip();

		if (normalized.length() < NICKNAME_MIN_LENGTH
			|| normalized.length() > NICKNAME_MAX_LENGTH
			|| !NICKNAME.matcher(normalized).matches()) {
			throw new MemberException(MemberErrorCode.INVALID_NICKNAME);
		}

		return normalized;
	}
}
