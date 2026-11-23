package xyz.ncookie.stargazer.domain.recommend.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.NightHour;
import xyz.ncookie.stargazer.domain.recommend.model.NightNote;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationLevel;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.weather.model.HourlyWeather;

/**
 * 실제 천문 계산을 붙여서, 밤의 길이와 달의 영향이 실제 날짜와 맞게 나오는지 확인한다.
 * 장소는 서울시청이고 하늘은 밤새 구름 없이 맑다고 둔다.
 */
class NightAssessmentEngineRealSkyTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final double LAT = 37.5665;
	private static final double LON = 126.9780;

	private final NightAssessmentEngine engine = new NightAssessmentEngine(new SkyEphemeris());

	@Test
	@DisplayName("11월 말 서울의 밤은 저녁 6시 칸부터 아침 6시 칸까지고, 저녁 7시 칸부터 새벽 5시 칸까지가 어둡다")
	void lateNovemberNight() {
		NightAssessment night = clearNight(LocalDate.of(2026, 11, 24));

		assertThat(night.hours()).extracting(hour -> hour.time().atZone(SEOUL).getHour())
			.containsExactly(18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6);
		assertThat(night.hours()).extracting(NightHour::dark).containsExactly(
			false, true, true, true, true, true, true, true, true, true, true, true, false);
	}

	@Test
	@DisplayName("하지 무렵의 밤은 짧다. 밤 10시 칸부터 새벽 3시 칸까지 여섯 칸만 어둡다")
	void midsummerNight() {
		NightAssessment night = clearNight(LocalDate.of(2026, 6, 21));

		List<Integer> darkHours = night.hours().stream()
			.filter(NightHour::dark)
			.map(hour -> hour.time().atZone(SEOUL).getHour())
			.toList();

		// 서울의 하지 무렵 항해박명은 밤 9시 조금 넘어 끝나고 새벽 4시 무렵 다시 시작한다.
		assertThat(darkHours).containsExactly(22, 23, 0, 1, 2, 3);
	}

	@Test
	@DisplayName("보름달이 뜬 2026-11-24 밤은 밤새 맑아도 한 단계 내려 가볼 만해요가 된다")
	void fullMoonNight() {
		NightAssessment night = clearNight(LocalDate.of(2026, 11, 24));

		assertThat(night.hours()).filteredOn(NightHour::dark)
			.allSatisfy(hour -> {
				assertThat(hour.moonUp()).isTrue();
				assertThat(hour.moonBrightness()).isGreaterThan(0.75);
			});
		assertThat(night.level()).isEqualTo(RecommendationLevel.GOOD);
		assertThat(night.notes()).containsExactly(NightNote.BRIGHT_MOON);
	}

	@Test
	@DisplayName("달이 없는 2026-11-09 밤은 밤새 맑으면 꼭 가볼 만해요다")
	void newMoonNight() {
		NightAssessment night = clearNight(LocalDate.of(2026, 11, 9));

		assertThat(night.hours()).filteredOn(NightHour::dark)
			.allSatisfy(hour -> assertThat(hour.moonBrightness()).isLessThan(0.01));
		assertThat(night.level()).isEqualTo(RecommendationLevel.STRONG);
		assertThat(night.notes()).isEmpty();
	}

	private NightAssessment clearNight(LocalDate date) {
		List<HourlyWeather> forecast = new ArrayList<>();
		Instant noon = ZonedDateTime.of(date, LocalTime.NOON, SEOUL).toInstant();
		for (int i = 0; i < 24; i++) {
			forecast.add(new HourlyWeather(noon.plusSeconds(3600L * i), 0, 0, 0, 0, 50, 0.0, 5.0, 20000));
		}
		return engine.assess(LAT, LON, date, SEOUL, 3, forecast, RecommendationPolicy.DEFAULT).orElseThrow();
	}
}
