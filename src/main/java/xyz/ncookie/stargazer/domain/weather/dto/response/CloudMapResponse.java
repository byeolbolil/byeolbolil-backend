package xyz.ncookie.stargazer.domain.weather.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.weather.model.CloudGridForecast;
import xyz.ncookie.stargazer.domain.weather.model.CloudGridSpec;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "하룻밤의 구름 격자 예보")
public record CloudMapResponse(

	@Schema(description = "밤이 시작하는 날짜", example = "2026-11-25")
	String date,

	@Schema(description = "예보 출처", example = "OPEN_METEO")
	String source,

	@Schema(description = "예보 모델이 계산을 시작한 시각(UTC). 확인하지 못했으면 빠진다", example = "2026-11-25T06:00:00Z")
	String issuedAt,

	Grid grid,

	@Schema(description = "저녁 5시부터 다음 날 아침 8시 전까지 한 시간 간격의 값. 예보가 닿지 않으면 빈 배열이다")
	List<Hour> hours
) {

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	@Schema(description = "격자. 남서쪽 모서리에서 시작해 step 간격으로 북쪽과 동쪽으로 간다")
	public record Grid(

		@Schema(description = "가장 남쪽 줄의 위도", example = "32.6")
		double south,

		@Schema(description = "가장 서쪽 칸의 경도", example = "124.2")
		double west,

		@Schema(description = "줄과 칸 사이의 간격(도)", example = "0.4")
		double step,

		@Schema(description = "줄의 수(남쪽에서 북쪽으로)", example = "17")
		int rows,

		@Schema(description = "칸의 수(서쪽에서 동쪽으로)", example = "18")
		int columns
	) {

		static Grid of(CloudGridSpec spec) {
			return new Grid(spec.south(), spec.west(), spec.step(), spec.rows(), spec.columns());
		}
	}

	@Schema(description = "한 시각의 격자 값. time부터 1시간이다")
	public record Hour(

		@Schema(description = "시각(한국 시간). 밤 추천의 hours[].time과 같은 형식이다", example = "2026-11-25T22:00:00+09:00")
		String time,

		@Schema(description = "총운량(%). 길이는 rows × columns이고, 남쪽 줄부터, 줄 안에서는 서쪽부터 적는다")
		List<Integer> cloudCover
	) {
	}

	public static CloudMapResponse of(LocalDate date, CloudGridForecast forecast, ZoneId zone) {
		Instant issuedAt = forecast.issuedAt();
		return new CloudMapResponse(
			date.toString(),
			forecast.source().name(),
			issuedAt != null ? issuedAt.toString() : null,
			Grid.of(forecast.grid()),
			forecast.frames().stream()
				.map(frame -> new Hour(
					TIME_FORMAT.format(frame.time().atZone(zone).toOffsetDateTime()), frame.cloudCover()))
				.toList()
		);
	}
}
