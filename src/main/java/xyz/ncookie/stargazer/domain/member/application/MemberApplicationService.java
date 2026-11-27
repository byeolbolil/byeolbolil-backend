package xyz.ncookie.stargazer.domain.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.application.dto.ModifyMemberCommand;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberValidationResponse;
import xyz.ncookie.stargazer.domain.member.entity.Member;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberApplicationService {

	private final MemberDomainService memberDomainService;

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
}
