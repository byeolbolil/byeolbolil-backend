package xyz.ncookie.stargazer.domain.geocode.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.geocode.application.GeocodeApplicationService;
import xyz.ncookie.stargazer.domain.geocode.dto.request.ReverseGeocodeRequest;
import xyz.ncookie.stargazer.domain.geocode.dto.request.SearchPlaceRequest;
import xyz.ncookie.stargazer.domain.geocode.dto.response.PlaceSearchResponse;
import xyz.ncookie.stargazer.domain.geocode.dto.response.ReverseGeocodeResponse;

@Tag(name = "지명 검색", description = "이름으로 좌표를 찾고, 좌표가 어느 지역인지 알려 준다")
@RestController
@RequestMapping("/api/v1/geocode")
@RequiredArgsConstructor
public class GeocodeController {

	private final GeocodeApplicationService geocodeApplicationService;

	@Operation(
		summary = "지명 검색",
		description = "한국 안의 지역, 산, 천문대, 역 같은 곳을 이름으로 찾는다. 지역과 산, 천문대를 앞에 두고 최대 8개를 "
			+ "돌려준다. 입력하는 도중에 불러도 된다. 인증 없이 쓸 수 있다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "찾은 장소. 없으면 빈 배열"),
		@ApiResponse(responseCode = "400", description = "검색어가 없거나 2자보다 짧거나 50자보다 길다"),
		@ApiResponse(responseCode = "429", description = "검색 요청이 몰렸다"),
		@ApiResponse(responseCode = "503", description = "지명 데이터 소스가 답하지 않는다")
	})
	@GetMapping("/search")
	public List<PlaceSearchResponse> search(@Valid @ModelAttribute SearchPlaceRequest request) {
		return geocodeApplicationService.search(request);
	}

	@Operation(
		summary = "좌표의 지역 이름",
		description = "좌표가 어느 지역인지 \"강릉시 왕산면\" 같은 이름으로 알려 준다. 인증 없이 쓸 수 있다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "지역 이름. 근처에 아무것도 없으면 name이 빠진다"),
		@ApiResponse(responseCode = "400", description = "좌표가 없거나 범위를 벗어났다"),
		@ApiResponse(responseCode = "429", description = "검색 요청이 몰렸다"),
		@ApiResponse(responseCode = "503", description = "지명 데이터 소스가 답하지 않는다")
	})
	@GetMapping("/reverse")
	public ReverseGeocodeResponse reverse(@Valid @ModelAttribute ReverseGeocodeRequest request) {
		return geocodeApplicationService.reverse(request);
	}
}
