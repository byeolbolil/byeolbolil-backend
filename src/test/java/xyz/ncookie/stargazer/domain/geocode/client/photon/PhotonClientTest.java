package xyz.ncookie.stargazer.domain.geocode.client.photon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;
import xyz.ncookie.stargazer.support.Fixtures;

/**
 * 2026-11-25에 실제로 받아 둔 응답(fixtures/photon)으로 확인한다.
 */
class PhotonClientTest {

	private MockRestServiceServer server;
	private PhotonClient client;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		client = new PhotonClient(restTemplate, "https://photon.komoot.io");
	}

	@Nested
	@DisplayName("이름으로 찾기")
	class Search {

		@Test
		@DisplayName("검색어를 인코딩하고, 한국 범위와 원래 언어의 이름을 30개까지 요청한다")
		void request() {
			server.expect(requestTo("https://photon.komoot.io/api/"
					+ "?q=%ED%99%94%EC%95%85%EC%82%B0&limit=30&lang=default&bbox=124.5,33.0,131.9,38.7"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(json("search-hwaaksan-20261125.json"));

			client.search("화악산");

			server.verify();
		}

		@Test
		@DisplayName("검색어의 &와 중괄호가 쿼리를 깨뜨리지 않는다")
		void specialCharacters() {
			server.expect(requestTo("https://photon.komoot.io/api/"
					+ "?q=a%26b%20%7Bc%7D&limit=30&lang=default&bbox=124.5,33.0,131.9,38.7"))
				.andRespond(json("search-no-result-20261125.json"));

			assertThat(client.search("a&b {c}")).isEmpty();
			server.verify();
		}

		@Test
		@DisplayName("이름, 큰 곳부터 적은 행정구역, 좌표를 읽는다. 응답의 좌표는 경도가 먼저다")
		void parse() {
			server.expect(anything()).andRespond(json("search-hwaaksan-20261125.json"));

			List<GeocodePlace> places = client.search("화악산");

			assertThat(places).hasSize(20);
			assertThat(places.get(0)).isEqualTo(new GeocodePlace(
				"화악산", List.of("경기도", "가평군", "북면"), GeocodePlace.Kind.PLACE, 37.9946674, 127.5034314));
		}

		@Test
		@DisplayName("산과 지역은 PLACE, 도로와 역은 OTHER로 나눈다")
		void kinds() {
			server.expect(anything()).andRespond(json("search-yangpyeong-20261125.json"));

			List<GeocodePlace> places = client.search("양평");

			// Photon은 이름이 똑같은 역을 맨 앞에, 양평군을 열두 번째에 준다
			assertThat(places.get(0).name()).isEqualTo("양평");
			assertThat(places.get(0).kind()).isEqualTo(GeocodePlace.Kind.OTHER);
			assertThat(places.get(11).name()).isEqualTo("양평군");
			assertThat(places.get(11).kind()).isEqualTo(GeocodePlace.Kind.PLACE);
		}

		@Test
		@DisplayName("시와 군이 같은 이름으로 두 번 오면 한 번만 적는다")
		void repeatedRegion() {
			server.expect(anything()).andRespond(json("search-yangpyeong-20261125.json"));

			GeocodePlace tree = client.search("양평").stream()
				.filter(place -> place.name().equals("양평 용문사 은행나무"))
				.findFirst().orElseThrow();

			assertThat(tree.regions()).containsExactly("경기도", "양평군");
		}

		@Test
		@DisplayName("이름이 없거나 한국 밖에 있는 결과는 버린다")
		void skipsUnusable() {
			server.expect(anything()).andRespond(withSuccess("""
				{"features": [
				  {"properties": {"name": "対馬", "countrycode": "JP", "type": "city"},
				   "geometry": {"coordinates": [129.29, 34.2]}},
				  {"properties": {"countrycode": "KR", "type": "street"},
				   "geometry": {"coordinates": [127.0, 37.5]}},
				  {"properties": {"name": "독도", "countrycode": "KR", "type": "locality"},
				   "geometry": {"coordinates": [131.8667, 37.2417]}}
				]}
				""", MediaType.APPLICATION_JSON));

			assertThat(client.search("섬")).extracting(GeocodePlace::name).containsExactly("독도");
		}

		@Test
		@DisplayName("결과가 없으면 빈 목록이다")
		void noResult() {
			server.expect(anything()).andRespond(json("search-no-result-20261125.json"));

			assertThat(client.search("육백마지기")).isEmpty();
		}

		@Test
		@DisplayName("서버 오류는 GeocodeProviderException이다")
		void serverError() {
			server.expect(anything()).andRespond(withServerError());

			assertThatThrownBy(() -> client.search("화악산")).isInstanceOf(GeocodeProviderException.class);
		}

		@Test
		@DisplayName("features가 없는 응답은 GeocodeProviderException이다")
		void malformed() {
			server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

			assertThatThrownBy(() -> client.search("화악산")).isInstanceOf(GeocodeProviderException.class);
		}
	}

	@Nested
	@DisplayName("좌표로 찾기")
	class Reverse {

		@Test
		@DisplayName("가장 가까운 한 곳을 반경 10km 안에서 요청한다")
		void request() {
			server.expect(requestTo("https://photon.komoot.io/reverse"
					+ "?lat=37.623&lon=128.739&limit=1&lang=default&radius=10"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(json("reverse-anbandegi-20261125.json"));

			client.reverse(37.623, 128.739);

			server.verify();
		}

		@Test
		@DisplayName("찾은 곳의 행정구역으로 지역 이름을 만들 수 있다")
		void parse() {
			server.expect(anything()).andRespond(json("reverse-anbandegi-20261125.json"));

			Optional<GeocodePlace> place = client.reverse(37.623, 128.739);

			assertThat(place).isPresent();
			assertThat(place.get().name()).isEqualTo("안반데기마을");
			assertThat(place.get().regions()).containsExactly("강원특별자치도", "강릉시", "왕산면");
			assertThat(place.get().areaName()).isEqualTo("강릉시 왕산면");
		}

		@Test
		@DisplayName("도가 없는 서울은 시와 동으로 이름을 만든다")
		void seoul() {
			server.expect(anything()).andRespond(json("reverse-seoul-20261125.json"));

			assertThat(client.reverse(37.57, 126.89)).map(GeocodePlace::areaName).contains("서울특별시 성산2동");
		}

		@Test
		@DisplayName("바다 한가운데처럼 근처에 아무것도 없으면 비어 있다")
		void nothingNearby() {
			server.expect(anything()).andRespond(json("reverse-sea-20261125.json"));

			assertThat(client.reverse(36.5, 130.5)).isEmpty();
		}
	}

	private static org.springframework.test.web.client.ResponseCreator json(String fixture) {
		return withSuccess(Fixtures.read("photon/" + fixture), MediaType.APPLICATION_JSON);
	}
}
