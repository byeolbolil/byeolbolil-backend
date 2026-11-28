package xyz.ncookie.stargazer.support;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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

import xyz.ncookie.stargazer.domain.weather.model.ForecastCell;

/**
 * 외부 API(Open-Meteo, MET Norway, Photon, 카카오 로컬)를 기록해 둔 응답으로 대체한다.
 *
 * 애플리케이션이 쓰는 RestTemplate 빈의 요청 팩토리를 바꿔 끼우므로, 이 빈으로 나가는 요청은
 * 소켓을 열지 않는다. 대체해 두지 않은 주소를 호출하면 AssertionError가 나서 테스트가 실패한다.
 * 클라이언트의 catch (Exception)에 삼켜지지 않으므로 빠뜨린 호출이 조용히 넘어가지 않는다.
 *
 * 한 테스트 안에서는 첫 요청이 나가기 전에 필요한 응답을 모두 등록해야 한다.
 * 도중에 응답을 바꾸려면 reset()을 부른 뒤 다시 등록한다.
 */
public class ExternalApiStubs {

	private static final String OPEN_METEO_FORECAST = "https://api.open-meteo.com/v1/forecast";
	private static final String OPEN_METEO_MODEL_META = "https://api.open-meteo.com/data/";
	private static final String MET_NORWAY = "https://api.met.no/weatherapi/locationforecast/2.0/complete";
	private static final String PHOTON_SEARCH = "https://photon.komoot.io/api/";
	private static final String PHOTON_REVERSE = "https://photon.komoot.io/reverse";
	private static final String KAKAO_KEYWORD = "https://dapi.kakao.com/v2/local/search/keyword.json";
	private static final String KAKAO_ADDRESS = "https://dapi.kakao.com/v2/local/search/address.json";
	private static final String RESEND_EMAILS = "https://api.resend.com/emails";
	private static final String KAKAO_UNLINK = "https://kapi.kakao.com/v1/user/unlink";
	private static final String NAVER_TOKEN = "https://nid.naver.com/oauth2.0/token";
	private static final String GOOGLE_REVOKE = "https://oauth2.googleapis.com/revoke";

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

	public void openMeteoForecastReturns(String fixture) {
		stub(HttpMethod.GET, OPEN_METEO_FORECAST, json("openmeteo/" + fixture));
	}

	/**
	 * 한 좌표가 속한 예보 칸에만 주는 응답. 칸마다 다른 날씨를 줄 때 쓴다.
	 * 먼저 등록한 응답이 먼저 맞춰지므로, openMeteoForecastReturnsBodyForOthers보다 앞에 등록해야 한다.
	 */
	public void openMeteoForecastReturnsBodyFor(double latitude, double longitude, String body) {
		ForecastCell cell = ForecastCell.of(latitude, longitude);
		String query = "latitude=" + cell.latitudeParam() + "&longitude=" + cell.longitudeParam() + "&";
		server.expect(ExpectedCount.manyTimes(), requestTo(allOf(startsWith(OPEN_METEO_FORECAST), containsString(query))))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	/** 따로 응답을 정하지 않은 나머지 모든 칸에 주는 응답. */
	public void openMeteoForecastReturnsBodyForOthers(String body) {
		stub(HttpMethod.GET, OPEN_METEO_FORECAST, withSuccess(body, MediaType.APPLICATION_JSON));
	}

	/**
	 * 구름 격자 요청에 주는 응답. 명소 예보와 주소가 같고 받는 변수만 달라서 hourly 값으로 구분한다.
	 * 명소 예보 응답과 함께 쓸 때는 이쪽을 먼저 등록해야 한다.
	 */
	public void openMeteoCloudGridReturnsBody(String body) {
		server.expect(ExpectedCount.manyTimes(),
				requestTo(allOf(startsWith(OPEN_METEO_FORECAST), containsString("&hourly=cloud_cover&"))))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
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

	public void photonSearchReturns(String fixture) {
		stub(HttpMethod.GET, PHOTON_SEARCH, json("photon/" + fixture));
	}

	public void photonSearchFails() {
		stub(HttpMethod.GET, PHOTON_SEARCH, withServerError());
	}

	public void photonReverseReturns(String fixture) {
		stub(HttpMethod.GET, PHOTON_REVERSE, json("photon/" + fixture));
	}

	public void kakaoKeywordReturns(String fixture) {
		stub(HttpMethod.GET, KAKAO_KEYWORD, json("kakao/" + fixture));
	}

	public void kakaoAddressReturns(String fixture) {
		stub(HttpMethod.GET, KAKAO_ADDRESS, json("kakao/" + fixture));
	}

	/** 카카오가 답은 하지만 찾은 곳이 없다 */
	public void kakaoFindsNothing() {
		kakaoKeywordReturns("keyword-no-result-20261127.json");
		kakaoAddressReturns("address-no-result-20261127.json");
	}

	public void kakaoFails() {
		stub(HttpMethod.GET, KAKAO_KEYWORD, withServerError());
		stub(HttpMethod.GET, KAKAO_ADDRESS, withServerError());
	}

	public void resendAccepts() {
		stub(HttpMethod.POST, RESEND_EMAILS, withSuccess("{\"id\":\"test-email-id\"}", MediaType.APPLICATION_JSON));
	}

	public void resendFails() {
		stub(HttpMethod.POST, RESEND_EMAILS, withServerError());
	}

	/** 카카오 연결 끊기. 그 회원의 액세스 토큰을 Bearer로 실어야 한다 */
	public void kakaoUnlinkAccepts(String accessToken) {
		server.expect(ExpectedCount.once(), requestTo(KAKAO_UNLINK))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andRespond(withSuccess("{\"id\":1234}", MediaType.APPLICATION_JSON));
	}

	public void kakaoUnlinkFails() {
		stub(HttpMethod.POST, KAKAO_UNLINK, withServerError());
	}

	/** 네이버 연동 해제. 앱 키와 회원의 액세스 토큰을 폼으로 보낸다 */
	public void naverUnlinkAccepts(String accessToken) {
		server.expect(ExpectedCount.once(), requestTo(NAVER_TOKEN))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().formDataContains(java.util.Map.of(
				"grant_type", "delete",
				"client_id", "test-naver-id",
				"client_secret", "test-naver-secret",
				"access_token", accessToken)))
			.andRespond(withSuccess("{\"access_token\":\"" + accessToken + "\",\"result\":\"success\"}",
				MediaType.APPLICATION_JSON));
	}

	/** 네이버는 실패해도 200으로 답하고 본문에 error를 싣는다 */
	public void naverUnlinkRejects() {
		stub(HttpMethod.POST, NAVER_TOKEN, withSuccess(
			"{\"error\":\"invalid_request\",\"error_description\":\"no valid data in session\"}", MediaType.APPLICATION_JSON));
	}

	public void googleRevokeAccepts(String accessToken) {
		server.expect(ExpectedCount.once(), requestTo(GOOGLE_REVOKE))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().formDataContains(java.util.Map.of("token", accessToken)))
			.andRespond(withSuccess());
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
