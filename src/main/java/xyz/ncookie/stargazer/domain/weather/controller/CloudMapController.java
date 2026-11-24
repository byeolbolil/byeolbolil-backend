package xyz.ncookie.stargazer.domain.weather.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.weather.application.CloudMapApplicationService;
import xyz.ncookie.stargazer.domain.weather.dto.response.CloudMapResponse;

@Tag(name = "구름 지도", description = "지도에 구름을 그리는 데 쓰는 격자 예보")
@RestController
@RequestMapping("/api/v1/clouds")
@RequiredArgsConstructor
public class CloudMapController {

	private final CloudMapApplicationService cloudMapApplicationService;

	@Operation(
		summary = "하룻밤의 구름 격자",
		description = "한국과 주변 바다를 0.4° 간격으로 나눈 격자의 시간별 총운량을 돌려준다. date에 시작하는 밤의 "
			+ "저녁 5시부터 다음 날 아침 8시 전까지다. 인증 없이 쓸 수 있다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "격자와 시간별 값. 예보가 닿지 않는 날짜면 hours가 빈 배열"),
		@ApiResponse(responseCode = "400", description = "date가 yyyy-MM-dd 형식이 아니다"),
		@ApiResponse(responseCode = "503", description = "격자 예보를 받지 못했다")
	})
	@GetMapping("/nights/{date}")
	public CloudMapResponse getNight(
		@Parameter(description = "밤이 시작하는 날짜", example = "2026-11-25")
		@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return cloudMapApplicationService.getNight(date);
	}
}
