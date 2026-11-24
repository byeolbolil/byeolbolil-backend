package xyz.ncookie.stargazer.domain.geocode.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import xyz.ncookie.stargazer.domain.geocode.cache.GeocodeCache;
import xyz.ncookie.stargazer.domain.geocode.client.GeocodeProviderException;
import xyz.ncookie.stargazer.domain.geocode.client.photon.PhotonClient;
import xyz.ncookie.stargazer.domain.geocode.exception.GeocodeErrorCode;
import xyz.ncookie.stargazer.domain.geocode.exception.GeocodeException;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace;
import xyz.ncookie.stargazer.domain.geocode.model.GeocodePlace.Kind;

class GeocodeDomainServiceTest {

	private static final Duration ONE_DAY = Duration.ofHours(24);

	private PhotonClient photonClient;
	private GeocodeCache cache;
	private GeocodeDomainService service;

	@BeforeEach
	void setUp() {
		photonClient = mock(PhotonClient.class);
		cache = mock(GeocodeCache.class);
		given(cache.findSearch(anyString())).willReturn(Optional.empty());
		given(cache.findReverse(anyString())).willReturn(Optional.empty());
		service = new GeocodeDomainService(photonClient, cache, bucketOf(100));
	}

	@Nested
	@DisplayName("결과 늘어놓기")
	class Arrange {

		@Test
		@DisplayName("지역과 산을 역과 도로보다 앞에 두고, 같은 종류 안에서는 받은 순서를 지킨다")
		void placesFirst() {
			List<GeocodePlace> arranged = GeocodeDomainService.arrange(List.of(
				place("양평", Kind.OTHER, 37.4928, 127.4919),
				place("양평군", Kind.PLACE, 37.4916, 127.4875),
				place("양평초등학교", Kind.OTHER, 37.4934, 127.4861),
				place("양평읍", Kind.PLACE, 37.4914, 127.4886)
			));

			assertThat(arranged).extracting(GeocodePlace::name)
				.containsExactly("양평군", "양평읍", "양평", "양평초등학교");
		}

		@Test
		@DisplayName("같은 이름이 1km 안에 또 있으면 앞의 것만 남긴다")
		void dropsNearbyDuplicates() {
			List<GeocodePlace> arranged = GeocodeDomainService.arrange(List.of(
				place("양평", Kind.OTHER, 37.4928, 127.4919),
				// 북쪽으로 0.9km
				place("양평", Kind.OTHER, 37.5009, 127.4919),
				// 북쪽으로 1.1km
				place("양평", Kind.OTHER, 37.5027, 127.4919)
			));

			assertThat(arranged).extracting(GeocodePlace::latitude).containsExactly(37.4928, 37.5027);
		}

		@Test
		@DisplayName("띄어쓰기만 다른 이름은 같은 이름으로 본다")
		void ignoresSpacesInNames() {
			List<GeocodePlace> arranged = GeocodeDomainService.arrange(List.of(
				place("별마로천문대", Kind.PLACE, 37.1934, 128.4955),
				place("별마로 천문대", Kind.OTHER, 37.1984, 128.4868)
			));

			assertThat(arranged).extracting(GeocodePlace::name).containsExactly("별마로천문대");
		}

		@Test
		@DisplayName("이름이 같아도 멀리 있으면 둘 다 남긴다")
		void keepsDistantNamesakes() {
			List<GeocodePlace> arranged = GeocodeDomainService.arrange(List.of(
				place("화악산", Kind.PLACE, 37.9947, 127.5034),
				place("화악산", Kind.PLACE, 35.5933, 128.6916)
			));

			assertThat(arranged).hasSize(2);
		}

		@Test
		@DisplayName("여덟 개까지만 돌려준다")
		void limit() {
			List<GeocodePlace> many = IntStream.range(0, 12)
				.mapToObj(index -> place("양평리" + index, Kind.PLACE, 35.0 + index, 127.0))
				.toList();

			assertThat(GeocodeDomainService.arrange(many)).hasSize(8)
				.extracting(GeocodePlace::name).startsWith("양평리0").endsWith("양평리7");
		}
	}

	@Nested
	@DisplayName("이름으로 찾기")
	class Search {

		@Test
		@DisplayName("앞뒤 공백, 여러 칸 띄어쓰기, 대소문자를 정리해서 찾고 하루 동안 기억한다")
		void normalizesAndCaches() {
			List<GeocodePlace> found = List.of(place("화악산", Kind.PLACE, 37.9947, 127.5034));
			given(photonClient.search("가평 화악산 n")).willReturn(found);

			assertThat(service.search("  가평   화악산 N ")).isEqualTo(found);
			verify(cache).putSearch("가평 화악산 n", found, ONE_DAY);
		}

		@Test
		@DisplayName("기억해 둔 검색어는 Photon을 부르지 않는다")
		void cacheHit() {
			List<GeocodePlace> remembered = List.of(place("화악산", Kind.PLACE, 37.9947, 127.5034));
			given(cache.findSearch("화악산")).willReturn(Optional.of(remembered));

			assertThat(service.search("화악산")).isEqualTo(remembered);
			verify(photonClient, never()).search(anyString());
		}

		@Test
		@DisplayName("결과가 없다는 답도 기억한다")
		void cachesEmptyResult() {
			given(photonClient.search("육백마지기")).willReturn(List.of());

			assertThat(service.search("육백마지기")).isEmpty();
			verify(cache).putSearch("육백마지기", List.of(), ONE_DAY);
		}

		@Test
		@DisplayName("Photon이 답하지 않으면 GEOCODE_UNAVAILABLE이고 아무것도 기억하지 않는다")
		void providerFailure() {
			given(photonClient.search("화악산")).willThrow(new GeocodeProviderException("시간 초과"));

			assertThatThrownBy(() -> service.search("화악산"))
				.isInstanceOfSatisfying(GeocodeException.class,
					e -> assertThat(e.getErrorCode()).isEqualTo(GeocodeErrorCode.GEOCODE_UNAVAILABLE));
			verify(cache, never()).putSearch(anyString(), any(), any());
		}

		@Test
		@DisplayName("나가는 호출이 상한에 닿으면 Photon을 부르지 않고 GEOCODE_BUSY다")
		void outboundLimit() {
			service = new GeocodeDomainService(photonClient, cache, bucketOf(1));
			given(photonClient.search(anyString())).willReturn(List.of());
			service.search("화악산");

			assertThatThrownBy(() -> service.search("명지산"))
				.isInstanceOfSatisfying(GeocodeException.class,
					e -> assertThat(e.getErrorCode()).isEqualTo(GeocodeErrorCode.GEOCODE_BUSY));
			verify(photonClient, never()).search("명지산");
		}

		@Test
		@DisplayName("기억해 둔 검색어는 상한에 닿은 뒤에도 답한다")
		void cacheHitIgnoresLimit() {
			service = new GeocodeDomainService(photonClient, cache, bucketOf(1));
			given(photonClient.search(anyString())).willReturn(List.of());
			service.search("화악산");
			given(cache.findSearch("명지산")).willReturn(Optional.of(List.of()));

			assertThat(service.search("명지산")).isEmpty();
		}
	}

	@Nested
	@DisplayName("좌표의 지역 이름")
	class AreaName {

		@Test
		@DisplayName("좌표를 소수 셋째 자리로 줄여 찾고 가장 작은 행정구역 둘로 답한다")
		void roundsAndNames() {
			GeocodePlace village = new GeocodePlace(
				"안반데기마을", List.of("강원특별자치도", "강릉시", "왕산면"), Kind.PLACE, 37.6228, 128.7393);
			given(photonClient.reverse(37.623, 128.739)).willReturn(Optional.of(village));

			assertThat(service.findAreaName(37.62279, 128.73927)).contains("강릉시 왕산면");
			verify(cache).putReverse("37.623,128.739", List.of(village), ONE_DAY);
		}

		@Test
		@DisplayName("근처에 아무것도 없다는 답도 기억한다")
		void cachesNothingNearby() {
			given(photonClient.reverse(anyDouble(), anyDouble())).willReturn(Optional.empty());

			assertThat(service.findAreaName(36.5, 130.5)).isEmpty();
			verify(cache).putReverse("36.500,130.500", List.of(), ONE_DAY);
		}

		@Test
		@DisplayName("기억해 둔 좌표는 Photon을 부르지 않는다")
		void cacheHit() {
			given(cache.findReverse("36.500,130.500")).willReturn(Optional.of(List.of()));

			assertThat(service.findAreaName(36.5, 130.5)).isEmpty();
			verify(photonClient, never()).reverse(anyDouble(), anyDouble());
		}

		@Test
		@DisplayName("행정구역을 모르는 곳은 장소 이름으로 답한다")
		void fallsBackToPlaceName() {
			given(photonClient.reverse(anyDouble(), anyDouble()))
				.willReturn(Optional.of(new GeocodePlace("독도", List.of(), Kind.PLACE, 37.2417, 131.8667)));

			assertThat(service.findAreaName(37.2417, 131.8667)).contains("독도");
		}
	}

	@Nested
	@DisplayName("주소")
	class Address {

		@Test
		@DisplayName("행정구역을 큰 곳부터 이어 붙인다")
		void joinsRegions() {
			GeocodePlace peak = new GeocodePlace("화악산", List.of("경기도", "가평군", "북면"), Kind.PLACE, 37.9947, 127.5034);

			assertThat(peak.address()).isEqualTo("경기도 가평군 북면");
		}

		@Test
		@DisplayName("이름과 같은 행정구역은 주소에서 뺀다")
		void omitsOwnName() {
			GeocodePlace town = new GeocodePlace("양평읍", List.of("경기도", "양평군", "양평읍"), Kind.PLACE, 37.4914, 127.4886);

			assertThat(town.address()).isEqualTo("경기도 양평군");
		}

		@Test
		@DisplayName("행정구역을 모르면 빈 문자열이다")
		void emptyWhenUnknown() {
			assertThat(new GeocodePlace("독도", List.of(), Kind.PLACE, 37.2417, 131.8667).address()).isEmpty();
		}
	}

	private static GeocodePlace place(String name, Kind kind, double latitude, double longitude) {
		return new GeocodePlace(name, List.of("경기도"), kind, latitude, longitude);
	}

	/** 다시 채워지지 않는 토큰 통. 테스트 안에서 시간이 흘러도 결과가 달라지지 않는다 */
	private static Bucket bucketOf(int tokens) {
		return Bucket.builder()
			.addLimit(Bandwidth.builder().capacity(tokens).refillIntervally(tokens, Duration.ofDays(1)).build())
			.build();
	}
}
