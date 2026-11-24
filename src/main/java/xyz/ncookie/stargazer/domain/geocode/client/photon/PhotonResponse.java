package xyz.ncookie.stargazer.domain.geocode.client.photon;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Photon의 /api와 /reverse 응답(GeoJSON FeatureCollection) 가운데 쓰는 부분.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotonResponse(List<Feature> features) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Feature(Properties properties, Geometry geometry) {}

	/**
	 * @param type   Photon이 매긴 단계. house, street, locality, district, city, county, state, country, other
	 * @param osmKey OpenStreetMap 태그의 키. natural, tourism, railway, highway 등
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Properties(
		String name,
		String type,
		@JsonProperty("osm_key") String osmKey,
		String countrycode,
		String state,
		String county,
		String city,
		String district
	) {}

	/**
	 * @param coordinates 경도, 위도 순서
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Geometry(List<Double> coordinates) {}
}
