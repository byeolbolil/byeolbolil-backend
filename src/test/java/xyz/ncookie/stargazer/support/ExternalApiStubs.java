package xyz.ncookie.stargazer.support;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API(OpenWeather, Gemini, Open-Meteo, MET Norway)를 기록해 둔 응답으로 대체한다.
 *
 * 애플리케이션이 쓰는 RestTemplate 빈의 요청 팩토리를 바꿔 끼우므로, 이 빈으로 나가는 요청은
 * 소켓을 열지 않는다. 대체해 두지 않은 주소를 호출하면 AssertionError가 나서 테스트가 실패한다.
 * 클라이언트의 catch (Exception)에 삼켜지지 않으므로 빠뜨린 호출이 조용히 넘어가지 않는다.
 *
 * 한 테스트 안에서는 첫 요청이 나가기 전에 필요한 응답을 모두 등록해야 한다.
 * 도중에 응답을 바꾸려면 reset()을 부른 뒤 다시 등록한다.
 */
public class ExternalApiStubs {

	private static final String OPENWEATHER_CURRENT = "https://api.openweathermap.org/data/2.5/weather";
	private static final String OPENWEATHER_FORECAST = "https://api.openweathermap.org/data/2.5/forecast";
	private static final String GEMINI = "https://generativelanguage.googleapis.com/v1beta/models/";
	private static final String OPEN_METEO_FORECAST = "https://api.open-meteo.com/v1/forecast";
	private static final String OPEN_METEO_MODEL_META = "https://api.open-meteo.com/data/";
	private static final String MET_NORWAY = "https://api.met.no/weatherapi/locationforecast/2.0/complete";

	private final MockRestServiceServer server;

	public ExternalApiStubs(RestTemplate restTemplate) {
		this.server = MockRestServiceServer.bindTo(restTemplate)
			.ignoreExpectOrder(true)
			.build();
	}

	/** 등록한 응답과 호출 기록을 지운다. 지운 뒤에는 어떤 외부 호출도 허용되지 않는다. */
	public void reset() {
		server.reset();
	}

	/** 등록한 응답이 모두 한 번 이상 호출됐는지 확인한다. */
	public void verifyAllCalled() {
		server.verify();
	}

	public void openWeatherCurrentReturns(String fixture) {
		stub(HttpMethod.GET, OPENWEATHER_CURRENT, json("openweather/" + fixture));
	}

	public void openWeatherCurrentFails() {
		stub(HttpMethod.GET, OPENWEATHER_CURRENT, withServerError());
	}

	public void openWeatherForecastReturns(String fixture) {
		stub(HttpMethod.GET, OPENWEATHER_FORECAST, json("openweather/" + fixture));
	}

	public void openWeatherForecastFails() {
		stub(HttpMethod.GET, OPENWEATHER_FORECAST, withServerError());
	}

	public void geminiReturns(String fixture) {
		stub(HttpMethod.POST, GEMINI, json("gemini/" + fixture));
	}

	public void geminiFails() {
		stub(HttpMethod.POST, GEMINI, withServerError());
	}

	public void openMeteoForecastReturns(String fixture) {
		stub(HttpMethod.GET, OPEN_METEO_FORECAST, json("openmeteo/" + fixture));
	}

	public void openMeteoForecastFails() {
		stub(HttpMethod.GET, OPEN_METEO_FORECAST, withServerError());
	}

	public void openMeteoModelMetaReturns(String fixture) {
		stub(HttpMethod.GET, OPEN_METEO_MODEL_META, json("openmeteo/" + fixture));
	}

	public void openMeteoModelMetaFails() {
		stub(HttpMethod.GET, OPEN_METEO_MODEL_META, withServerError());
	}

	/**
	 * @param expires 응답에 실을 Expires 헤더. 예: "Mon, 23 Nov 2026 19:18:57 GMT"
	 */
	public void metNorwayReturns(String fixture, String expires) {
		stub(HttpMethod.GET, MET_NORWAY,
			withSuccess(Fixtures.read("metnorway/" + fixture), MediaType.APPLICATION_JSON)
				.header(HttpHeaders.EXPIRES, expires));
	}

	public void metNorwayFails() {
		stub(HttpMethod.GET, MET_NORWAY, withServerError());
	}

	private void stub(HttpMethod httpMethod, String urlPrefix, ResponseCreator response) {
		server.expect(ExpectedCount.manyTimes(), requestTo(startsWith(urlPrefix)))
			.andExpect(method(httpMethod))
			.andRespond(response);
	}

	private ResponseCreator json(String fixturePath) {
		return withSuccess(Fixtures.read(fixturePath), MediaType.APPLICATION_JSON);
	}
}
