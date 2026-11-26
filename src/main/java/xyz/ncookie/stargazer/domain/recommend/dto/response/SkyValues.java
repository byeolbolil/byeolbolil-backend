package xyz.ncookie.stargazer.domain.recommend.dto.response;

import xyz.ncookie.stargazer.domain.recommend.model.LightCondition;
import xyz.ncookie.stargazer.domain.recommend.model.SkyClass;

/**
 * 하늘 밝기를 응답에 싣는 모양. 밤 추천과 이번 주 응답이 같은 값을 준다.
 */
final class SkyValues {

	private SkyValues() {
	}

	/** 소수 둘째 자리까지. 모형의 정밀도가 그보다 높지 않다 */
	static Double brightness(LightCondition light) {
		if (light.skyBrightness() == null) {
			return null;
		}
		return Math.round(light.skyBrightness() * 100) / 100.0;
	}

	static Integer rank(LightCondition light) {
		return light.skyRank();
	}

	static String className(LightCondition light) {
		SkyClass skyClass = light.skyClass();
		return (skyClass != null) ? skyClass.name() : null;
	}
}
