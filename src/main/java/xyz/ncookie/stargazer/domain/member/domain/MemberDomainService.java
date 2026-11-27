package xyz.ncookie.stargazer.domain.member.domain;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.entity.Role;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberDomainService {

	static final int NICKNAME_MIN_LENGTH = 2;
	static final int NICKNAME_MAX_LENGTH = 16;

	// 한글 음절과 자모, 영문, 숫자, 밑줄로 된 낱말을 공백 하나로 잇는다(docs/decisions.md 2026-11-27 "닉네임 수정")
	private static final Pattern NICKNAME = Pattern.compile("[가-힣ㄱ-ㅣA-Za-z0-9_]+( [가-힣ㄱ-ㅣA-Za-z0-9_]+)*");

	// 소셜 가입과 관리자 초기화 때 주는 기본 닉네임. 네 자리로 몇 번 뽑아 보고, 모두 겹치면 자리를 늘린다
	static final String DEFAULT_NICKNAME_PREFIX = "별지기";
	private static final int[] DEFAULT_NICKNAME_DIGITS = {4, 4, 4, 4, 4, 6, 6, 6, 8, 8};

	private final MemberRepository memberRepository;

	private final RandomGenerator random = new SecureRandom();

	public Member findById(Long memberId) {

		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	public boolean isEmailDuplicated(String email) {
		return memberRepository.existsByEmail(email);
	}

	/**
	 * 회원이 정하는 닉네임을 검사하고 저장할 값을 돌려준다. 규칙에 어긋나면 400, 다른 회원이 쓰고 있으면 409다.
	 * 가입할 때는 {@code memberId}가 null이고, 바꿀 때는 자기 닉네임과는 겹쳐도 된다(대소문자만 바꾸는 경우).
	 */
	public String validateNewNickname(String nickname, Long memberId) {

		String normalized = normalizeNickname(nickname);
		if (isTaken(normalized, memberId)) {
			throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATED);
		}
		return normalized;
	}

	/** 규칙에 어긋나면 400을 던지고, 맞으면 다른 회원이 쓰고 있지 않은지 돌려준다 */
	public boolean isNicknameAvailable(String nickname, Long memberId) {
		return !isTaken(normalizeNickname(nickname), memberId);
	}

	/**
	 * 규칙은 바꿀 때만 본다. 가입할 때 받은 닉네임은 규칙에 어긋나도 그대로 둔다.
	 */
	public void changeNickname(Member member, String nickname) {
		member.changeNickname(validateNewNickname(nickname, member.getId()));
		try {
			memberRepository.flush();
		} catch (DataIntegrityViolationException e) {
			throw nicknameConflictOr(e);
		}
	}

	/**
	 * 검사를 지나고도 같은 순간에 다른 요청이 같은 닉네임을 저장해 DB 유일 제약에 걸렸으면 409로 바꾼다.
	 * 다른 제약(이메일 등)이면 그대로 돌려준다.
	 */
	public RuntimeException nicknameConflictOr(DataIntegrityViolationException e) {

		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation
				&& violation.getConstraintName() != null
				&& violation.getConstraintName().toLowerCase().contains(Member.NICKNAME_UNIQUE)) {
				return new MemberException(MemberErrorCode.NICKNAME_DUPLICATED);
			}
		}
		return e;
	}

	/** 부적절한 닉네임을 기본 닉네임으로 되돌린다 */
	public void resetNickname(Member member) {
		member.changeNickname(generateDefaultNickname());
	}

	/** 아직 아무도 쓰지 않는 "별지기1234" 꼴의 닉네임 */
	public String generateDefaultNickname() {

		for (int digits : DEFAULT_NICKNAME_DIGITS) {
			int bound = (int)Math.pow(10, digits);
			String candidate = DEFAULT_NICKNAME_PREFIX + String.format("%0" + digits + "d", random.nextInt(bound));
			if (!memberRepository.existsByNickname(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("기본 닉네임을 정하지 못했습니다");
	}

	public void validateAdmin(Member member) {

		if (member.getRole() != Role.ADMIN) {
			throw new MemberException(MemberErrorCode.ADMIN_ONLY);
		}
	}

	private boolean isTaken(String nickname, Long memberId) {
		return memberId == null
			? memberRepository.existsByNickname(nickname)
			: memberRepository.existsByNicknameAndIdNot(nickname, memberId);
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
