package xyz.ncookie.stargazer.domain.member.application.dto;

import xyz.ncookie.stargazer.domain.member.dto.request.ModifyMemberRequest;

public record ModifyMemberCommand(
	String nickname
) {

	public static ModifyMemberCommand from(ModifyMemberRequest request) {
		return new ModifyMemberCommand(request.nickname());
	}
}
