package xyz.ncookie.stargazer.domain.geocode.application;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.geocode.domain.GeocodeDomainService;
import xyz.ncookie.stargazer.domain.geocode.dto.request.ReverseGeocodeRequest;
import xyz.ncookie.stargazer.domain.geocode.dto.request.SearchPlaceRequest;
import xyz.ncookie.stargazer.domain.geocode.dto.response.PlaceSearchResponse;
import xyz.ncookie.stargazer.domain.geocode.dto.response.ReverseGeocodeResponse;

@Service
@RequiredArgsConstructor
public class GeocodeApplicationService {

	private final GeocodeDomainService geocodeDomainService;

	public List<PlaceSearchResponse> search(SearchPlaceRequest request) {
		return geocodeDomainService.search(request.q()).stream()
			.map(PlaceSearchResponse::from)
			.toList();
	}

	public ReverseGeocodeResponse reverse(ReverseGeocodeRequest request) {
		return new ReverseGeocodeResponse(
			geocodeDomainService.findAreaName(request.lat(), request.lon()).orElse(null));
	}
}
