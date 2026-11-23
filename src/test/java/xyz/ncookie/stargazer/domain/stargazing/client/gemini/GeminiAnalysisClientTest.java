package xyz.ncookie.stargazer.domain.stargazing.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.model.GeminiAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.support.Fixtures;

/**
 * Gemini 응답을 읽는 방식과, 호출이 실패했을 때 내보내는 대체값을 고정한다.
 */
class GeminiAnalysisClientTest {

	private static final int SCORE = 94;

	private MockRestServiceServer server;
	private GeminiAnalysisClient client;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		client = new GeminiAnalysisClient(restTemplate);
		ReflectionTestUtils.setField(client, "geminiKey", "test-gemini-key");
	}

	@Test
	@DisplayName("코드 블록으로 감싼 JSON에서 한 줄 평과 등급 문구를 읽는다")
	void parsesFencedJson() {
		server.expect(anything())
			.andRespond(withSuccess(Fixtures.read("gemini/comment-good.json"), MediaType.APPLICATION_JSON));

		GeminiAnalysisResult result = analyze();

		assertThat(result.comment()).isEqualTo("하늘이 맑고 어두워서 은하수까지 기대해 볼 만한 밤이에요.");
		assertThat(result.bortleClass()).isEqualTo("Class 2");
		assertThat(result.brightness()).isEqualTo("매우 어두움");
		assertThat(result.limitingMag()).isEqualTo("6.5등급");
	}

	@Test
	@DisplayName("API 키를 쿼리 문자열에 실어 POST로 보내고, 본문에 점수와 광해 등급을 넣는다")
	void request() {
		server.expect(requestTo(containsString(":generateContent?key=test-gemini-key")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString("최종 관측 점수: 94점")))
			.andExpect(content().string(containsString("Bortle Class 2")))
			.andRespond(withSuccess(Fixtures.read("gemini/comment-good.json"), MediaType.APPLICATION_JSON));

		analyze();

		server.verify();
	}

	@Test
	@DisplayName("서버 오류가 나면 예외를 던지지 않고 대체 문구를 돌려준다. 점수는 넘겨받은 값 그대로다")
	void serverErrorFallsBack() {
		server.expect(anything()).andRespond(withServerError());

		assertFallback(analyze());
	}

	@Test
	@DisplayName("모델이 없다는 404에도 같은 대체 문구를 돌려준다")
	void notFoundFallsBack() {
		server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertFallback(analyze());
	}

	@Test
	@DisplayName("응답 본문이 JSON이 아니면 대체 문구를 돌려준다")
	void nonJsonTextFallsBack() {
		server.expect(anything())
			.andRespond(withSuccess(Fixtures.read("gemini/comment-not-json.json"), MediaType.APPLICATION_JSON));

		assertFallback(analyze());
	}

	@Test
	@DisplayName("후보가 비어 있으면 대체 문구를 돌려준다")
	void emptyCandidatesFallsBack() {
		server.expect(anything())
			.andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

		assertFallback(analyze());
	}

	private GeminiAnalysisResult analyze() {
		OpenWeatherResponse weather = new OpenWeatherResponse(
			new OpenWeatherResponse.Main(0.0, 50.0),
			new OpenWeatherResponse.Clouds(0),
			10000,
			null
		);
		RawAstronomyData astro = new RawAstronomyData(0.1, 143.0, -70.0, "07:37", "17:30", "05:47", "14:49");

		return client.getAnalysis(SCORE, List.of("광해 등급 Class 2 (-6점)"), 37.6254, 128.9392, weather, astro, 2);
	}

	private static void assertFallback(GeminiAnalysisResult result) {
		assertThat(result.finalScore()).isEqualTo(SCORE);
		assertThat(result.comment()).isEqualTo("AI 연결 지연");
		assertThat(result.bortleClass()).isEqualTo("Class ?");
		assertThat(result.brightness()).isEqualTo("알 수 없음");
		assertThat(result.limitingMag()).isEqualTo("?등급");
	}
}
