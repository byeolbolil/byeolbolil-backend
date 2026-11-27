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
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetMoonCalendarCommand;
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetNightRecommendationCommand;
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetSkyCommand;
import xyz.ncookie.stargazer.domain.recommend.application.dto.GetWeekOutlookCommand;
import xyz.ncookie.stargazer.domain.recommend.domain.NightRecommendationDomainService;
import xyz.ncookie.stargazer.domain.recommend.dto.response.MoonCalendarResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightRecommendationResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.NightSummaryResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.SkyResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.WeekOutlookResponse;
import xyz.ncookie.stargazer.domain.recommend.exception.RecommendErrorCode;
import xyz.ncookie.stargazer.domain.recommend.exception.RecommendException;
import xyz.ncookie.stargazer.domain.recommend.model.CandidatePlace;
import xyz.ncookie.stargazer.domain.recommend.model.ClosedPeriod;
import xyz.ncookie.stargazer.domain.recommend.model.LightCondition;
import xyz.ncookie.stargazer.domain.recommend.model.MoonNight;
import xyz.ncookie.stargazer.domain.recommend.model.NightAssessment;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceGuide;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceLink;
import xyz.ncookie.stargazer.domain.recommend.model.PlacePoint;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceVideo;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceRecommendation;
import xyz.ncookie.stargazer.domain.recommend.model.PlaceType;
import xyz.ncookie.stargazer.domain.recommend.model.RecommendationPolicy;
import xyz.ncookie.stargazer.domain.recommend.model.TravelEstimate;
import xyz.ncookie.stargazer.domain.recommend.model.WeekOutlook;
import xyz.ncookie.stargazer.domain.spot.domain.ObservationSpotDomainService;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;
import xyz.ncookie.stargazer.domain.skylight.domain.SkyLightDomainService;
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

	// 정책: 달력은 오늘 밤부터 6주. 삭망월(약 29.5일)보다 길어서 어느 날 열어도 다음 그믐과 그 앞뒤 밤이 들어간다.
	private static final int MOON_CALENDAR_DAYS = 42;

	private final NightRecommendationDomainService nightRecommendationDomainService;
	private final WeatherDomainService weatherDomainService;
	private final SkyLightDomainService skyLightDomainService;
	private final BookmarkDomainService bookmarkDomainService;
	private final ObservationSpotDomainService observationSpotDomainService;
	private final Executor recommendTaskExecutor;
	private final Clock clock;

	public NightRecommendationApplicationService(
		NightRecommendationDomainService nightRecommendationDomainService,
		WeatherDomainService weatherDomainService,
		SkyLightDomainService skyLightDomainService,
		BookmarkDomainService bookmarkDomainService,
		ObservationSpotDomainService observationSpotDomainService,
		@Qualifier("recommendTaskExecutor") Executor recommendTaskExecutor,
		Clock clock
	) {
		this.nightRecommendationDomainService = nightRecommendationDomainService;
		this.weatherDomainService = weatherDomainService;
		this.skyLightDomainService = skyLightDomainService;
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

		// 그 밤에 길이 막혀 있는 명소는 후보에서 뺀다
		List<CandidatePlace> candidates = candidates(command.memberId()).stream()
			.filter(place -> !place.isClosedOn(date))
			.toList();

		// 외부 호출인 예보를 병렬로 받는다.
		List<CompletableFuture<Optional<PlaceRecommendation>>> futures = new ArrayList<>();
		for (CandidatePlace place : candidates) {
			LightCondition light = lightOf(place);
			futures.add(CompletableFuture.supplyAsync(
				() -> evaluate(place, date, light, command), recommendTaskExecutor));
		}

		List<PlaceRecommendation> evaluated = futures.stream()
			.map(CompletableFuture::join)
			.flatMap(Optional::stream)
			.toList();

		List<PlaceRecommendation> withinReach = evaluated.stream()
			.filter(recommendation -> nightRecommendationDomainService.isWithinReach(
				recommendation.travel(), command.maxTravelMinutes()))
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

	/**
	 * 오늘 밤부터 7일의 밤마다 후보 가운데 가장 좋은 등급. 밤을 고르기 전에 어느 밤이 좋은지 보여 주는 데 쓴다.
	 * 후보와 이동 시간 한도를 밤 추천과 같게 적용하므로, 밤마다의 등급은 그 밤 추천의 첫 장소 등급과 같다.
	 * 예보를 받지 못한 장소는 빼고, 평가한 장소가 하나도 없는 밤은 등급 없이 돌려준다.
	 */
	@Transactional(readOnly = true)
	public NightSummaryResponse summarize(GetNightRecommendationCommand command) {
		LocalDate tonight = tonight();

		// 이동 시간은 날짜와 상관없으므로, 한도를 넘는 곳은 예보를 받기 전에 뺀다.
		List<CandidatePlace> withinReach = candidates(command.memberId()).stream()
			.filter(place -> nightRecommendationDomainService.isWithinReach(
				nightRecommendationDomainService.travelTo(place, command.originLatitude(), command.originLongitude()),
				command.maxTravelMinutes()))
			.toList();

		List<CompletableFuture<List<NightAssessment>>> futures = new ArrayList<>();
		for (CandidatePlace place : withinReach) {
			LightCondition light = lightOf(place);
			// 길이 막혀 있는 밤은 그 장소의 평가에서 뺀다. 밤 추천에서 그 장소가 빠지는 것과 맞춘다
			futures.add(CompletableFuture.supplyAsync(
				() -> outlookOrEmpty(place, light, tonight, command.policy()).nights().stream()
					.filter(night -> !place.isClosedOn(night.date()))
					.toList(),
				recommendTaskExecutor));
		}
		List<List<NightAssessment>> nightsByPlace = futures.stream().map(CompletableFuture::join).toList();

		return NightSummaryResponse.of(
			tonight,
			SELECTABLE_DAYS,
			command.policy(),
			command.maxTravelMinutes(),
			nightRecommendationDomainService.bestLevelByNight(nightsByPlace),
			nightRecommendationDomainService.goodCountByNight(nightsByPlace)
		);
	}

	/**
	 * 좌표로 고른 한 위치의 이번 주 밤. 예보를 받지 못하면 예외로 알린다.
	 */
	public WeekOutlookResponse weekAt(GetWeekOutlookCommand command) {
		if (command.latitude() == null || command.longitude() == null) {
			throw new RecommendException(RecommendErrorCode.LOCATION_REQUIRED);
		}
		LocalDate tonight = tonight();
		CandidatePlace place = new CandidatePlace(null, null, null, null, null, command.latitude(), command.longitude());
		WeatherForecast forecast = weatherDomainService.getHourlyForecast(place.latitude(), place.longitude());

		WeekOutlook outlook = outlookOf(place, lightOf(place), forecast, tonight, command.policy());
		return WeekOutlookResponse.of(tonight, SELECTABLE_DAYS, command.policy(), List.of(outlook), ZONE);
	}

	/**
	 * 한 좌표의 하늘 밝기. 지도에서 고른 자리가 얼마나 어두운지 보여 줄 때 쓴다. 예보를 받지 않으므로 가볍다.
	 */
	public SkyResponse skyAt(GetSkyCommand command) {
		CandidatePlace place = new CandidatePlace(null, null, null, null, null, command.latitude(), command.longitude());
		return SkyResponse.of(command.latitude(), command.longitude(), lightOf(place));
	}

	/**
	 * 한 좌표에서 본 앞으로 6주의 달. 예보를 보지 않으므로 날씨가 닿지 않는 날까지 계획할 수 있다.
	 */
	public MoonCalendarResponse moonCalendarAt(GetMoonCalendarCommand command) {
		List<MoonNight> nights = nightRecommendationDomainService.moonNights(
			command.latitude(), command.longitude(), tonight(), MOON_CALENDAR_DAYS, ZONE);
		return MoonCalendarResponse.of(command.latitude(), command.longitude(), nights, ZONE);
	}

	/**
	 * 저장한 장소들의 이번 주 밤. 로그인하지 않았으면 장소가 없다.
	 * 예보를 받지 못한 장소는 빼지 않고 밤이 없는 채로 돌려준다. 저장한 곳이 목록에서 사라지면 지워진 것처럼 보인다.
	 */
	@Transactional(readOnly = true)
	public WeekOutlookResponse weekOfMyPlaces(GetWeekOutlookCommand command) {
		LocalDate tonight = tonight();
		List<CandidatePlace> places = (command.memberId() != null) ? bookmarkPlaces(command.memberId()) : List.of();

		List<CompletableFuture<WeekOutlook>> futures = new ArrayList<>();
		for (CandidatePlace place : places) {
			LightCondition light = lightOf(place);
			futures.add(CompletableFuture.supplyAsync(
				() -> outlookOrEmpty(place, light, tonight, command.policy()), recommendTaskExecutor));
		}
		List<WeekOutlook> outlooks = futures.stream().map(CompletableFuture::join).toList();

		return WeekOutlookResponse.of(tonight, SELECTABLE_DAYS, command.policy(), outlooks, ZONE);
	}

	private WeekOutlook outlookOrEmpty(CandidatePlace place, LightCondition light, LocalDate tonight, RecommendationPolicy policy) {
		try {
			WeatherForecast forecast = weatherDomainService.getHourlyForecast(place.latitude(), place.longitude());
			return outlookOf(place, light, forecast, tonight, policy);
		} catch (WeatherException e) {
			log.warn("예보를 받지 못해 이번 주 밤을 비워 둡니다. place={}", place.name());
			return new WeekOutlook(place, light, List.of(), null, null);
		}
	}

	private WeekOutlook outlookOf(
		CandidatePlace place, LightCondition light, WeatherForecast forecast, LocalDate tonight, RecommendationPolicy policy
	) {
		List<NightAssessment> nights = nightRecommendationDomainService.assessNights(
			place.latitude(), place.longitude(), tonight, SELECTABLE_DAYS, ZONE, light, forecast, policy);
		return new WeekOutlook(place, light, nights, forecast.source(), forecast.issuedAt());
	}

	/**
	 * 장소의 불빛 조건. 하늘 밝기와 둘레의 불빛은 좌표로 찾고, 직접 보이는 먼 불빛은 명소에 계산해 둔 값을 쓴다.
	 * 자료가 있는 범위 밖의 좌표는 모르는 채로 둔다.
	 */
	private LightCondition lightOf(CandidatePlace place) {
		return skyLightDomainService.findSkyLight(place.latitude(), place.longitude())
			.map(sky -> new LightCondition(sky.skyBrightness(), sky.nearbyRadiance(), place.lightsInView(),
				skyLightDomainService.koreaRankOf(sky.skyBrightness())))
			.orElseGet(() -> new LightCondition(null, null, place.lightsInView()));
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
			for (CandidatePlace place : bookmarkPlaces(memberId)) {
				if (place.spotId() != null) {
					bookmarkedSpotIds.add(place.spotId());
				}
				candidates.add(place);
			}
		}

		// 기본 후보는 밤에 자유롭게 갈 수 있는 관측지뿐이다. 천문대 같은 체험 시설은 직접 저장했을 때만 평가한다
		for (ObservationSpot spot : observationSpotDomainService.findRecommendable()) {
			if (bookmarkedSpotIds.contains(spot.getId())) {
				continue;
			}
			candidates.add(new CandidatePlace(
				PlaceType.SPOT, spot.getId(), spot.getId(), spot.getTitle(), spot.getAddress(),
				spot.getLatitude(), spot.getLongitude(), guideOf(spot), spot.getLightsInView(), closedPeriodOf(spot)));
		}
		return candidates;
	}

	/** 회원이 저장한 장소. 좌표가 없는 북마크는 평가할 수 없어 뺀다 */
	private List<CandidatePlace> bookmarkPlaces(Long memberId) {
		List<CandidatePlace> places = new ArrayList<>();
		for (Bookmark bookmark : bookmarkDomainService.findAllByMemberId(memberId)) {
			if (bookmark.getLatitude() == null || bookmark.getLongitude() == null) {
				continue;
			}
			Long spotId = (bookmark.getSpot() != null) ? bookmark.getSpot().getId() : null;
			places.add(new CandidatePlace(
				PlaceType.BOOKMARK, bookmark.getId(), spotId, bookmark.getName(), bookmark.getAddress(),
				bookmark.getLatitude(), bookmark.getLongitude(),
				bookmark.getSpot() != null ? guideOf(bookmark.getSpot()) : null,
				bookmark.getSpot() != null ? bookmark.getSpot().getLightsInView() : null,
				null));
		}
		return places;
	}

	private static ClosedPeriod closedPeriodOf(ObservationSpot spot) {
		if (spot.getClosedFrom() == null || spot.getClosedUntil() == null) {
			return null;
		}
		return new ClosedPeriod(spot.getClosedFrom(), spot.getClosedUntil());
	}

	private static PlaceGuide guideOf(ObservationSpot spot) {
		List<PlaceLink> links = spot.getLinks().stream()
			.map(link -> new PlaceLink(link.getTitle(), link.getUrl(), link.getImageUrl()))
			.toList();
		List<PlacePoint> points = spot.getPoints().stream()
			.map(point -> new PlacePoint(point.getName(), point.getAccess(), point.getLatitude(), point.getLongitude()))
			.toList();
		List<PlaceVideo> videos = spot.getVideos().stream()
			.map(video -> new PlaceVideo(
				video.getTitle(), video.getChannel(), video.getUrl(), video.getThumbnailUrl(), video.isShortForm()))
			.toList();
		return new PlaceGuide(
			spot.getViewpoint(), spot.getDescription(), spot.getGuide(), spot.getCaution(), links, points, videos);
	}

	/**
	 * 한 장소의 예보를 받아 평가한다. 예보를 받지 못한 장소는 전체를 실패시키지 않고 결과에서 뺀다.
	 */
	private Optional<PlaceRecommendation> evaluate(
		CandidatePlace place, LocalDate date, LightCondition light, GetNightRecommendationCommand command
	) {
		try {
			WeatherForecast forecast = weatherDomainService.getHourlyForecast(place.latitude(), place.longitude());
			return nightRecommendationDomainService.evaluate(
				place, date, ZONE, light, forecast,
				command.originLatitude(), command.originLongitude(), command.policy());
		} catch (WeatherException e) {
			log.warn("예보를 받지 못해 추천에서 뺍니다. place={}", place.name());
			return Optional.empty();
		}
	}
}
