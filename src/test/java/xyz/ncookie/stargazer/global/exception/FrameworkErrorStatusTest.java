package xyz.ncookie.stargazer.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * Spring이 상태 코드를 정해 둔 예외가 500이 아니라 그 상태로 나가는지 본다.
 */
class FrameworkErrorStatusTest extends IntegrationTestSupport {

	@Test
	@DisplayName("없는 경로는 404이고 공통 응답 형식으로 답한다")
	void unknownPath() {
		MvcTestResult result = mvc.get().uri("/").exchange();

		assertThat(result).hasStatus(404).bodyJson().isLenientlyEqualTo("""
			{
			  "success": false,
			  "status": 404,
			  "message": "요청한 경로를 찾을 수 없습니다."
			}
			""");
	}

	@Test
	@DisplayName("지원하지 않는 메서드는 405다")
	void methodNotSupported() {
		MvcTestResult result = mvc.post().uri("/api/v1/spots").exchange();

		assertThat(result).hasStatus(405).bodyJson().isLenientlyEqualTo("""
			{
			  "success": false,
			  "status": 405,
			  "message": "Method 'POST' is not supported."
			}
			""");
	}
}
