package xyz.ncookie.stargazer.domain.skylight.domain;

import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.skylight.model.SkyLight;
import xyz.ncookie.stargazer.domain.skylight.provider.SkyBrightnessGrid;

/**
 * 좌표의 불빛 자료. 밤 추천이 장소마다 하늘 밝기와 둘레의 불빛을 찾을 때 쓴다.
 */
@Service
@RequiredArgsConstructor
public class SkyLightDomainService {

	private final SkyBrightnessGrid skyBrightnessGrid;

	/**
	 * 좌표의 하늘 밝기와 둘레의 불빛. 자료가 있는 범위(한국 둘레) 밖이면 비어 있다.
	 */
	public Optional<SkyLight> findSkyLight(double lat, double lon) {
		return skyBrightnessGrid.lookup(lat, lon);
	}
}
