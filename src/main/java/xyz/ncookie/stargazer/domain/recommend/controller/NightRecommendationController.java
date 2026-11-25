package xyz.ncookie.stargazer.domain.recommend.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.application.NightRecommendationApplicationService;
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetNightRecommendationCommand;
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetWeekOutlookCommand;
import xyz.ncookie.stargazer.domain.recommend.dto.request.GetNightRecommendationRequest;
import xyz.ncookie.stargazer.domain.recommend.dto.request.GetWeekOutlookRequest;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightSummaryResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.WeekOutlookResponse;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@Tag(name = "밤 추천", description = "오늘 밤과 이번 주에 어디서 언제 별을 보기 좋은지 추천한다")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class NightRecommendationController {

	private final NightRecommendationApplicationService nightRecommendationApplicationService;

	@Operation(
		summary = "오늘 밤 추천",
		description = "기본 추천 명소를 오늘 밤 좋은 순서로 돌려준다. 로그인했으면 내 장소도 함께 본다. "
			+ "새벽 4시 전에는 지난 저녁에 시작한 밤이 오늘 밤이다. 인증 없이 쓸 수 있다."
	)
	@GetMapping("/tonight")
	public NightRecommendationResponse getTonight(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @ModelAttribute GetNightRecommendationRequest request
	) {
		return nightRecommendationApplicationService.recommend(
			GetNightRecommendationCommand.from(memberIdOf(principal), null, request));
	}

	@Operation(
		summary = "날짜별 밤 추천",
		description = "date에 시작하는 밤의 추천을 돌려준다. 오늘 밤부터 7일 안의 날짜만 볼 수 있다. 인증 없이 쓸 수 있다."
	)
	@GetMapping("/nights/{date}")
	public NightRecommendationResponse getByDate(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "밤이 시작하는 날짜(yyyy-MM-dd)", example = "2026-11-24")
		@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@Valid @ModelAttribute GetNightRecommendationRequest request
	) {
		return nightRecommendationApplicationService.recommend(
			GetNightRecommendationCommand.from(memberIdOf(principal), date, request));
	}

	@Operation(
		summary = "밤마다 가장 좋은 등급",
		description = "오늘 밤부터 7일의 밤마다 후보 가운데 가장 좋은 등급을 돌려준다. 밤을 고르는 버튼에 그 밤이 "
			+ "얼마나 좋은지 미리 보여 주는 데 쓴다. 쿼리와 후보는 밤 추천과 같다. 인증 없이 쓸 수 있다."
	)
	@GetMapping("/nights")
	public NightSummaryResponse getNights(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @ModelAttribute GetNightRecommendationRequest request
	) {
		return nightRecommendationApplicationService.summarize(
			GetNightRecommendationCommand.from(memberIdOf(principal), null, request));
	}

	@Operation(
		summary = "한 위치의 이번 주 밤",
		description = "lat, lon으로 고른 위치의 오늘 밤부터 7일을 밤마다 평가해 돌려준다. 명소가 아니어도 된다. "
			+ "이동 시간은 보지 않는다. 인증 없이 쓸 수 있다."
	)
	@GetMapping("/week")
	public WeekOutlookResponse getWeek(@Valid @ModelAttribute GetWeekOutlookRequest request) {
		return nightRecommendationApplicationService.weekAt(GetWeekOutlookCommand.forLocation(request));
	}

	@Operation(
		summary = "내 장소의 이번 주 밤",
		description = "저장한 장소마다 오늘 밤부터 7일을 평가해 돌려준다. lat, lon은 쓰지 않는다. "
			+ "로그인하지 않았으면 장소가 없는 응답이 온다."
	)
	@GetMapping("/week/mine")
	public WeekOutlookResponse getMyWeek(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @ModelAttribute GetWeekOutlookRequest request
	) {
		return nightRecommendationApplicationService.weekOfMyPlaces(
			GetWeekOutlookCommand.forMember(memberIdOf(principal), request));
	}

	private static Long memberIdOf(MemberPrincipal principal) {
		return (principal != null) ? principal.getMemberId() : null;
	}
}
