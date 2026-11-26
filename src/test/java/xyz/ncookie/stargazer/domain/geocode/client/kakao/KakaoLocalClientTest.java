package xyz.ncookie.stargazer.domain.geocode.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace.Kind;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace.Source;
import xyz.ncookie.stargazer.support.Fixtures;

/**
 * 2026-11-27에 실제로 받아 둔 응답(fixtures/kakao)으로 확인한다.
 */
class KakaoLocalClientTest {

	private MockRestServiceServer server;
	private KakaoLocalClient client;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		client = new KakaoLocalClient(restTemplate, "https://dapi.kakao.com", "test-key");
	}

	@Test
	@DisplayName("키가 비어 있으면 쓰지 않는다")
	void notConfigured() {
		assertThat(client.isConfigured()).isTrue();
		assertThat(new KakaoLocalClient(new RestTemplate(), "https://dapi.kakao.com", " ").isConfigured()).isFalse();
		assertThat(new KakaoLocalClient(new RestTemplate(), "https://dapi.kakao.com", null).isConfigured()).isFalse();
	}

	@Nested
	@DisplayName("이름으로 장소 찾기")
	class Places {

		@Test
		@DisplayName("검색어를 인코딩해 15개까지 요청하고, 키를 KakaoAK 헤더로 보낸다")
		void request() {
			server.expect(requestTo("https://dapi.kakao.com/v2/local/search/keyword.json"
					+ "?query=%ED%99%94%EC%95%85%EC%82%B0&size=15"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "KakaoAK test-key"))
				.andRespond(json("keyword-hwaaksan-20261127.json"));

			client.searchPlaces("화악산");

			server.verify();
		}

		@Test
		@DisplayName("이름, 지번을 뗀 행정구역, 일곱 자리로 줄인 좌표를 받고 출처는 카카오다")
		void mapsDocuments() {
			server.expect(anything()).andRespond(json("keyword-manghyang-20261127.json"));

			List<GeocodePlace> places = client.searchPlaces("정천면망향의광장전망대");

			assertThat(places).containsExactly(new GeocodePlace(
				"정천면망향의광장", List.of("전북특별자치도", "진안군", "정천면", "모정리"), Kind.PLACE,
				35.90085, 127.4836018, Source.KAKAO));
			assertThat(places.get(0).address()).isEqualTo("전북특별자치도 진안군 정천면 모정리");
		}

		@Test
		@DisplayName("관광지와 등산로, 출입로는 목적지로, 숙소와 종교 시설은 나머지로 나눈다. 받은 순서는 그대로다")
		void kinds() {
			server.expect(anything()).andRespond(json("keyword-hwaaksan-20261127.json"));

			List<GeocodePlace> places = client.searchPlaces("화악산");

			assertThat(places).hasSize(15);
			assertThat(places).extracting(GeocodePlace::name)
				.startsWith("화악산", "화악산", "화악산 출입로", "화악산등산코스", "화악산계곡", "화악산소나무리조트");
			assertThat(places).extracting(GeocodePlace::kind)
				.startsWith(Kind.PLACE, Kind.PLACE, Kind.PLACE, Kind.PLACE, Kind.PLACE, Kind.OTHER, Kind.OTHER,
					Kind.PLACE, Kind.OTHER);
		}

		@Test
		@DisplayName("찾은 곳이 없으면 빈 목록이다")
		void noResult() {
			server.expect(anything()).andRespond(json("keyword-no-result-20261127.json"));

			assertThat(client.searchPlaces("전북 진안군 정천면 970-40")).isEmpty();
		}

		@Test
		@DisplayName("카카오가 거절하면 GeocodeProviderException이다")
		void rejected() {
			server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"errorType\":\"AccessDeniedError\",\"message\":\"ip mismatched!\"}"));

			assertThatThrownBy(() -> client.searchPlaces("화악산")).isInstanceOf(GeocodeProviderException.class);
		}
	}

	@Nested
	@DisplayName("주소로 찾기")
	class Addresses {

		@Test
		@DisplayName("주소 검색 주소로 5개까지 요청한다")
		void request() {
			server.expect(requestTo("https://dapi.kakao.com/v2/local/search/address.json"
					+ "?query=%EB%8C%80%EC%A0%84&size=5"))
				.andExpect(header("Authorization", "KakaoAK test-key"))
				.andRespond(json("address-daejeon-20261127.json"));

			client.searchAddresses("대전");

			server.verify();
		}

		@Test
		@DisplayName("지번 주소는 카카오가 정리한 전체 주소가 이름이 된다")
		void jibun() {
			server.expect(anything()).andRespond(json("address-jibun-20261127.json"));

			List<GeocodePlace> places = client.searchAddresses("전북 진안군 정천면 970-40");

			assertThat(places).containsExactly(new GeocodePlace(
				"전북특별자치도 진안군 정천면 모정리 970-40", List.of(), Kind.PLACE, 35.9009409, 127.4834921, Source.KAKAO));
			assertThat(places.get(0).address()).isEmpty();
		}

		@Test
		@DisplayName("시 이름은 그 지역의 대표 좌표로 답한다")
		void region() {
			server.expect(anything()).andRespond(json("address-daejeon-20261127.json"));

			assertThat(client.searchAddresses("대전")).extracting(GeocodePlace::name).containsExactly("대전");
		}
	}

	@Nested
	@DisplayName("지번 주소에서 행정구역만 남기기")
	class Regions {

		@Test
		@DisplayName("번지와 '산'부터는 버린다")
		void dropsLotNumber() {
			assertThat(KakaoLocalClient.regionsOf("경기 가평군 북면 화악리 산 228-5")).containsExactly("경기", "가평군", "북면", "화악리");
			assertThat(KakaoLocalClient.regionsOf("대전 서구 만년동 396")).containsExactly("대전", "서구", "만년동");
			assertThat(KakaoLocalClient.regionsOf("경기 양평군 양서면 양수리")).containsExactly("경기", "양평군", "양서면", "양수리");
		}

		@Test
		@DisplayName("주소가 없으면 빈 목록이다")
		void blank() {
			assertThat(KakaoLocalClient.regionsOf(null)).isEmpty();
			assertThat(KakaoLocalClient.regionsOf("  ")).isEmpty();
		}
	}

	private static ResponseCreator json(String fixture) {
		return withSuccess(Fixtures.read("kakao/" + fixture), MediaType.APPLICATION_JSON);
	}
}
