package xyz.ncookie.stargazer.domain.recommend.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.bookmark.domain.BookmarkDomainService;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetNightRecommendationCommand;
import xyz.ncookie.stargazer.domain.recommend.domain.NightRecommendationDomainService;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse;
import xyz.ncookie.stargazer.domain.recommend.exception.RecommendErrorCode;
import xyz.ncookie.stargazer.domain.recommend.exception.RecommendException;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceGuide;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceLink;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceType;
import xyz.ncookie.stargazer.domain.spot.domain.ObservationSpotDomainService;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;
import xyz.ncookie.stargazer.domain.stargazing.domain.StargazingDomainService;
import xyz.ncookie.stargazer.domain.weather.domain.WeatherDomainService;
import xyz.ncookie.stargazer.domain.weather.exception.WeatherException;
import xyz.ncookie.stargazer.domain.weather.model.WeatherForecast;

/**
 * 하룻밤의 장소별 추천을 만든다. 후보는 기본 추천 명소와, 로그인했다면 내 장소다.
 */
@Slf4j
@Service
public class NightRecommendationApplicationService {

	// 지금은 국내 장소만 다루므로 날짜와 시각을 모두 한국 시간으로 읽는다.
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	// 정책: 새벽 4시 전에는 아직 지난 저녁에 시작한 밤을 "오늘 밤"으로 본다. 그 뒤로는 다가오는 밤이다.
	private static final int NIGHT_ROLLOVER_HOUR = 4;

	// 오늘 밤을 포함해 7일. 예보를 168시간 받으므로 그 안에서만 날짜를 고를 수 있다.
	private static final int SELECTABLE_DAYS = 7;

	private final NightRecommendationDomainService nightRecommendationDomainService;
	private final WeatherDomainService weatherDomainService;
	private final StargazingDomainService stargazingDomainService;
	private final BookmarkDomainService bookmarkDomainService;
	private final ObservationSpotDomainService observationSpotDomainService;
	private final Executor recommendTaskExecutor;
	private final Clock clock;

	public NightRecommendationApplicationService(
		NightRecommendationDomainService nightRecommendationDomainService,
		WeatherDomainService weatherDomainService,
		StargazingDomainService stargazingDomainService,
		BookmarkDomainService bookmarkDomainService,
		ObservationSpotDomainService observationSpotDomainService,
		@Qualifier("recommendTaskExecutor") Executor recommendTaskExecutor,
		Clock clock
	) {
		this.nightRecommendationDomainService = nightRecommendationDomainService;
		this.weatherDomainService = weatherDomainService;
		this.stargazingDomainService = stargazingDomainService;
		this.bookmarkDomainService = bookmarkDomainService;
		this.observationSpotDomainService = observationSpotDomainService;
		this.recommendTaskExecutor = recommendTaskExecutor;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public NightRecommendationResponse recommend(GetNightRecommendationCommand command) {
		LocalDate tonight = tonight();
		LocalDate date = (command.date() != null) ? command.date() : tonight;
		if (date.isBefore(tonight) || !date.isBefore(tonight.plusDays(SELECTABLE_DAYS))) {
			throw new RecommendException(RecommendErrorCode.DATE_OUT_OF_RANGE);
		}

		List<CandidatePlace> candidates = candidates(command.memberId());

		// 광해 등급은 DB 조회라 요청 스레드에서 먼저 구하고, 외부 호출인 예보만 병렬로 받는다.
		List<CompletableFuture<Optional<PlaceRecommendation>>> futures = new ArrayList<>();
		for (CandidatePlace place : candidates) {
			int bortleClass = stargazingDomainService.getBortleClass(place.latitude(), place.longitude());
			futures.add(CompletableFuture.supplyAsync(
				() -> evaluate(place, date, bortleClass, command), recommendTaskExecutor));
		}

		List<PlaceRecommendation> evaluated = futures.stream()
			.map(CompletableFuture::join)
			.flatMap(Optional::stream)
			.toList();

		List<PlaceRecommendation> withinReach = evaluated.stream()
			.filter(recommendation -> isWithinReach(recommendation, command.maxTravelMinutes()))
			.toList();

		List<NightRecommendationResponse.Place> places = nightRecommendationDomainService.rank(withinReach).stream()
			.map(recommendation -> NightRecommendationResponse.Place.of(recommendation, ZONE))
			.toList();

		return new NightRecommendationResponse(
			date.toString(),
			NightRecommendationResponse.Criteria.of(command.policy(), command.maxTravelMinutes()),
			candidates.size(),
			evaluated.size(),
			evaluated.size() - withinReach.size(),
			places
		);
	}

	private LocalDate tonight() {
		ZonedDateTime now = clock.instant().atZone(ZONE);
		return (now.getHour() < NIGHT_ROLLOVER_HOUR) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
	}

	/**
	 * 내 장소를 먼저 담고, 내 장소로 이미 들어간 명소는 기본 추천 명소에서 뺀다.
	 */
	private List<CandidatePlace> candidates(Long memberId) {
		List<CandidatePlace> candidates = new ArrayList<>();
		Set<Long> bookmarkedSpotIds = new HashSet<>();

		if (memberId != null) {
			for (Bookmark bookmark : bookmarkDomainService.findAllByMemberId(memberId)) {
				if (bookmark.getLatitude() == null || bookmark.getLongitude() == null) {
					continue;
				}
				Long spotId = (bookmark.getSpot() != null) ? bookmark.getSpot().getId() : null;
				if (spotId != null) {
					bookmarkedSpotIds.add(spotId);
				}
				candidates.add(new CandidatePlace(
					PlaceType.BOOKMARK, bookmark.getId(), spotId, bookmark.getName(), bookmark.getAddress(),
					bookmark.getLatitude(), bookmark.getLongitude(),
					bookmark.getSpot() != null ? guideOf(bookmark.getSpot()) : null));
			}
		}

		// 기본 후보는 밤에 자유롭게 갈 수 있는 관측지뿐이다. 천문대 같은 체험 시설은 직접 저장했을 때만 평가한다
		for (ObservationSpot spot : observationSpotDomainService.findRecommendable()) {
			if (bookmarkedSpotIds.contains(spot.getId())) {
				continue;
			}
			candidates.add(new CandidatePlace(
				PlaceType.SPOT, spot.getId(), spot.getId(), spot.getTitle(), spot.getAddress(),
				spot.getLatitude(), spot.getLongitude(), guideOf(spot)));
		}
		return candidates;
	}

	private static PlaceGuide guideOf(ObservationSpot spot) {
		List<PlaceLink> links = spot.getLinks().stream()
			.map(link -> new PlaceLink(link.getTitle(), link.getUrl(), link.getImageUrl()))
			.toList();
		return new PlaceGuide(spot.getViewpoint(), spot.getDescription(), spot.getGuide(), spot.getCaution(), links);
	}

	/**
	 * 한 장소의 예보를 받아 평가한다. 예보를 받지 못한 장소는 전체를 실패시키지 않고 결과에서 뺀다.
	 */
	private Optional<PlaceRecommendation> evaluate(
		CandidatePlace place, LocalDate date, int bortleClass, GetNightRecommendationCommand command
	) {
		try {
			WeatherForecast forecast = weatherDomainService.getHourlyForecast(place.latitude(), place.longitude());
			return nightRecommendationDomainService.evaluate(
				place, date, ZONE, bortleClass, forecast,
				command.originLatitude(), command.originLongitude(), command.policy());
		} catch (WeatherException e) {
			log.warn("예보를 받지 못해 추천에서 뺍니다. place={}", place.name());
			return Optional.empty();
		}
	}

	private static boolean isWithinReach(PlaceRecommendation recommendation, Integer maxTravelMinutes) {
		if (maxTravelMinutes == null || recommendation.travel() == null) {
			return true;
		}
		return recommendation.travel().minutes() <= maxTravelMinutes;
	}
}
