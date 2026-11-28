package xyz.ncookie.stargazer.domain.skyevent.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.ncookie.stargazer.domain.skyevent.model.EclipseKind;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEventType;

class SkyEventCatalogTest {

	@Test
	@DisplayName("실제 목록을 읽으면 날짜순이고 2026년 쌍둥이자리 유성우가 들어 있다")
	void readsRealCatalog() {
		SkyEventCatalog catalog = new SkyEventCatalog();

		assertThat(catalog.all()).isSortedAccordingTo((a, b) -> a.night().compareTo(b.night()));
		SkyEvent geminids = catalog.all().stream()
			.filter(event -> event.id().equals("2026-geminids"))
			.findFirst().orElseThrow();
		assertThat(geminids.type()).isEqualTo(SkyEventType.METEOR_SHOWER);
		assertThat(geminids.night()).isEqualTo(LocalDate.of(2026, 12, 14));
		assertThat(geminids.peak()).isEqualTo(Instant.parse("2026-12-14T14:00:00Z"));
		assertThat(geminids.radiantRa()).isEqualTo(112.0);
		assertThat(geminids.radiantDec()).isEqualTo(33.0);
		assertThat(geminids.zhr()).isEqualTo(150);
	}

	@Test
	@DisplayName("월식 줄은 종류와 시작, 끝 시각을 읽고, 출처 칸에는 쉼표가 들어가도 된다")
	void parsesEclipse() {
		SkyEvent event = SkyEventCatalog.parse(
			"e,LUNAR_ECLIPSE,개기월식,2026-03-03,2026-03-03T11:33:42Z,,,,TOTAL,"
				+ "2026-03-03T09:49:48Z,2026-03-03T13:17:36Z,천문연, 보도자료", 1);

		assertThat(event.eclipseKind()).isEqualTo(EclipseKind.TOTAL);
		assertThat(event.hasRadiant()).isFalse();
		assertThat(event.source()).isEqualTo("천문연, 보도자료");
	}

	@Test
	@DisplayName("복사점이 없는 유성우 줄은 시작할 때 실패한다")
	void meteorShowerNeedsRadiant() {
		assertThatThrownBy(() -> SkyEventCatalog.parse(
			"m,METEOR_SHOWER,유성우,2026-12-14,2026-12-14T14:00:00Z,,,150,,,,출처", 7))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("7번째 줄");
	}

	@Test
	@DisplayName("칸 수가 모자라면 실패한다")
	void wrongColumnCount() {
		assertThatThrownBy(() -> SkyEventCatalog.parse("m,METEOR_SHOWER,유성우", 3))
			.isInstanceOf(IllegalStateException.class);
	}
}
