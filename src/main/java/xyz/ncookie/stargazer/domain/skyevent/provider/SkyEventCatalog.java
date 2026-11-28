package xyz.ncookie.stargazer.domain.skyevent.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.domain.skyevent.model.EclipseKind;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEvent;
import xyz.ncookie.stargazer.domain.skyevent.model.SkyEventType;

/**
 * 천문 현상 목록. 시작할 때 CSV 한 장을 읽어 메모리에 둔다.
 *
 * 목록은 해마다 12월에 한국천문연구원이 다음 해의 "주요 천문현상"을 내면 손으로 옮겨 적는다. 목록이 끝나면 현상이
 * 나오지 않을 뿐 다른 기능은 그대로다. 줄의 형식은 CSV 첫머리의 주석에 적어 둔다.
 */
@Component
public class SkyEventCatalog {

	static final String RESOURCE = "skyevents/sky_events.csv";
	private static final int COLUMNS = 12;

	private final List<SkyEvent> events;

	public SkyEventCatalog() {
		this(new ClassPathResource(RESOURCE));
	}

	SkyEventCatalog(ClassPathResource resource) {
		List<SkyEvent> read = new ArrayList<>();
		int lineNumber = 0;
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				read.add(parse(line, lineNumber));
			}
		} catch (IOException e) {
			throw new UncheckedIOException("천문 현상 목록을 읽지 못했습니다: " + resource.getPath(), e);
		}
		validateUniqueIds(read);
		read.sort(Comparator.comparing(SkyEvent::night).thenComparing(SkyEvent::peak));
		this.events = List.copyOf(read);
	}

	/** 밤의 날짜순 */
	public List<SkyEvent> all() {
		return events;
	}

	/**
	 * id,type,name,night,peak,radiant_ra,radiant_dec,zhr,eclipse_kind,start,end,source.
	 * 마지막 칸인 출처에는 쉼표가 들어가도 된다.
	 */
	static SkyEvent parse(String line, int lineNumber) {
		String[] c = line.split(",", COLUMNS);
		if (c.length != COLUMNS) {
			throw invalid(lineNumber, COLUMNS + "칸이어야 합니다");
		}
		try {
			SkyEvent event = new SkyEvent(
				required(c[0], "id", lineNumber),
				SkyEventType.valueOf(c[1].trim()),
				required(c[2], "name", lineNumber),
				LocalDate.parse(c[3].trim()),
				Instant.parse(c[4].trim()),
				optionalDouble(c[5]),
				optionalDouble(c[6]),
				optionalInteger(c[7]),
				c[8].isBlank() ? null : EclipseKind.valueOf(c[8].trim()),
				c[9].isBlank() ? null : Instant.parse(c[9].trim()),
				c[10].isBlank() ? null : Instant.parse(c[10].trim()),
				required(c[11], "source", lineNumber)
			);
			validate(event, lineNumber);
			return event;
		} catch (IllegalArgumentException | java.time.DateTimeException e) {
			throw invalid(lineNumber, e.getMessage());
		}
	}

	private static void validate(SkyEvent event, int lineNumber) {
		if (event.type() == SkyEventType.METEOR_SHOWER && (!event.hasRadiant() || event.zhr() == null)) {
			throw invalid(lineNumber, "유성우에는 복사점과 ZHR이 있어야 합니다");
		}
		if (event.type() == SkyEventType.LUNAR_ECLIPSE
			&& (event.eclipseKind() == null || !event.hasEclipseSpan() || !event.start().isBefore(event.end()))) {
			throw invalid(lineNumber, "월식에는 종류와 시작, 끝 시각이 있어야 합니다");
		}
	}

	private static void validateUniqueIds(List<SkyEvent> events) {
		Set<String> ids = new HashSet<>();
		for (SkyEvent event : events) {
			if (!ids.add(event.id())) {
				throw new IllegalStateException("천문 현상 목록에 같은 id가 두 번 있습니다: " + event.id());
			}
		}
	}

	private static String required(String value, String column, int lineNumber) {
		if (value.isBlank()) {
			throw invalid(lineNumber, column + " 칸이 비어 있습니다");
		}
		return value.trim();
	}

	private static Double optionalDouble(String value) {
		return value.isBlank() ? null : Double.valueOf(value.trim());
	}

	private static Integer optionalInteger(String value) {
		return value.isBlank() ? null : Integer.valueOf(value.trim());
	}

	private static IllegalStateException invalid(int lineNumber, String reason) {
		return new IllegalStateException("천문 현상 목록 " + lineNumber + "번째 줄을 읽지 못했습니다: " + reason);
	}
}
