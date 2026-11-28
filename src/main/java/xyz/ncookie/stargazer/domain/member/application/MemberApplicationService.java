package xyz.ncookie.stargazer.domain.member.application;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.application.dto.ModifyMemberCommand;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberValidationResponse;
import xyz.ncookie.stargazer.domain.bookmark.domain.BookmarkDomainService;
import xyz.ncookie.stargazer.domain.member.domain.AuthDomainService;
import xyz.ncookie.stargazer.domain.member.dto.response.WithdrawalTicketResponse;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.WithdrawalTicketRedisRepository;
import xyz.ncookie.stargazer.global.security.oauth.SocialUnlinkClient;
import xyz.ncookie.stargazer.global.security.oauth.WithdrawalAuthorizationRequestResolver;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberApplicationService {

	// 탈퇴 표는 받은 뒤 곧바로 제공자 로그인으로 넘어가므로 길 필요가 없다. 보안 기준에서 온 값은 아니다
	private static final Duration WITHDRAWAL_TICKET_TTL = Duration.ofMinutes(5);

	private final MemberDomainService memberDomainService;
	private final AuthDomainService authDomainService;
	private final BookmarkDomainService bookmarkDomainService;
	private final WithdrawalTicketRedisRepository withdrawalTicketRedisRepository;
	private final RefreshTokenRedisRepository refreshTokenRedisRepository;
	private final SocialUnlinkClient socialUnlinkClient;

	@Transactional(readOnly = true)
	public MemberInfoResponse getMyInfo(Long memberId) {

		Member member = memberDomainService.findById(memberId);
		return MemberInfoResponse.from(member);
	}

	@Transactional
	public MemberInfoResponse modifyMyInfo(Long memberId, ModifyMemberCommand command) {

		Member member = memberDomainService.findById(memberId);
		memberDomainService.changeNickname(member, command.nickname());
		return MemberInfoResponse.from(member);
	}

	/** 로그인한 채로 물으면 자기 닉네임은 쓸 수 있다고 답한다 */
	@Transactional(readOnly = true)
	public MemberValidationResponse checkNickname(String nickname, Long memberId) {

		return new MemberValidationResponse(memberDomainService.isNicknameAvailable(nickname, memberId));
	}

	/** 부적절한 닉네임을 관리자가 기본 닉네임으로 되돌린다 */
	@Transactional
	public MemberInfoResponse resetNickname(Long adminId, Long targetId) {

		memberDomainService.validateAdmin(memberDomainService.findById(adminId));
		Member target = memberDomainService.findById(targetId);
		memberDomainService.resetNickname(target);
		log.info("관리자 {}가 회원 {}의 닉네임을 기본 닉네임으로 되돌렸다", adminId, targetId);
		return MemberInfoResponse.from(target);
	}

	@Transactional(readOnly = true)
	public MemberValidationResponse validateEmailDuplicated(String email) {

		boolean isDuplicated = memberDomainService.isEmailDuplicated(email);
		return new MemberValidationResponse(!isDuplicated);
	}

	/**
	 * 이메일 회원의 탈퇴. 비밀번호를 다시 확인하고, 이 요청에 실린 리프레시 토큰도 지운다.
	 */
	@Transactional
	public void withdrawLocal(Long memberId, String password, String refreshToken) {

		Member member = memberDomainService.findById(memberId);
		if (member.getAuthProvider() != AuthProvider.LOCAL) {
			throw new MemberException(MemberErrorCode.WITHDRAWAL_NEEDS_PROVIDER);
		}
		authDomainService.verifyPassword(member, password);

		delete(member);
		if (refreshToken != null) {
			refreshTokenRedisRepository.delete(refreshToken);
		}
		log.info("회원 {} 탈퇴 (이메일)", memberId);
	}

	/**
	 * 소셜 회원이 탈퇴하려면 가입한 제공자에 다시 로그인한다. 그 로그인 주소에 실을 일회용 표를 만든다.
	 * 표 없이 제공자 로그인만으로는 탈퇴되지 않게 하려는 것이다(docs/decisions.md 2026-11-28 "회원 탈퇴는 제공자 로그인을 한 번 더 거친다").
	 */
	public WithdrawalTicketResponse issueWithdrawalTicket(Long memberId) {

		Member member = memberDomainService.findById(memberId);
		if (member.getAuthProvider() == AuthProvider.LOCAL) {
			throw new MemberException(MemberErrorCode.WITHDRAWAL_NEEDS_PASSWORD);
		}

		String ticket = UUID.randomUUID().toString();
		withdrawalTicketRedisRepository.save(ticket, memberId, WITHDRAWAL_TICKET_TTL);
		return new WithdrawalTicketResponse("/oauth2/authorization/" + member.getAuthProvider().name().toLowerCase()
			+ "?" + WithdrawalAuthorizationRequestResolver.WITHDRAW_PARAMETER + "=" + ticket);
	}

	/**
	 * 탈퇴 표를 들고 제공자 로그인을 마친 소셜 회원을 탈퇴시킨다. 표의 회원과 방금 로그인한 계정이 같아야 한다.
	 * 제공자 연결 끊기는 실패해도 탈퇴를 막지 않는다.
	 *
	 * @param accessToken 방금 로그인에서 받은 제공자의 액세스 토큰
	 * @return 탈퇴한 회원 ID
	 */
	@Transactional
	public Long withdrawSocial(String ticket, AuthProvider provider, String providerId, String accessToken) {

		Long ticketMemberId = withdrawalTicketRedisRepository.consume(ticket);
		Member member = memberDomainService.findByProvider(provider, providerId).orElse(null);
		if (ticketMemberId == null || member == null || !member.getId().equals(ticketMemberId)) {
			throw new MemberException(MemberErrorCode.WITHDRAWAL_REJECTED);
		}

		boolean unlinked = socialUnlinkClient.unlink(provider, accessToken);
		delete(member);
		log.info("회원 {} 탈퇴 ({}, 연결 끊기 {})", member.getId(), provider, unlinked ? "성공" : "실패");
		return member.getId();
	}

	/** 북마크, 회원, 그 회원의 모든 기기의 리프레시 토큰을 지운다 */
	private void delete(Member member) {

		bookmarkDomainService.deleteAllByMemberId(member.getId());
		memberDomainService.delete(member);
		refreshTokenRedisRepository.deleteAllOf(member.getId());
	}
}
