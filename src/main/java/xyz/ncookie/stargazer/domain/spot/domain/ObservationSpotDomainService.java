package xyz.ncookie.stargazer.domain.spot.domain;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;
import xyz.ncookie.stargazer.domain.spot.exception.ObservationSpotErrorCode;
import xyz.ncookie.stargazer.domain.spot.exception.ObservationSpotException;
import xyz.ncookie.stargazer.domain.spot.repository.ObservationRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObservationSpotDomainService {

	private final ObservationRepository observationRepository;

	public List<ObservationSpot> findAll() {
		return observationRepository.findAll();
	}

	/** 밤 추천의 후보. 지금 보여 주는 열린 관측지만이다. 천문대 같은 체험 시설과 뺀 곳은 들어가지 않는다 */
	public List<ObservationSpot> findRecommendable() {
		return observationRepository.findAll().stream()
			.filter(ObservationSpot::isRecommendable)
			.toList();
	}

	public ObservationSpot findById(Long spotId) {

		return observationRepository.findById(spotId)
			.orElseThrow(() -> new ObservationSpotException(ObservationSpotErrorCode.SPOT_NOT_FOUND, spotId.toString()));
	}
}
