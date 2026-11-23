package xyz.ncookie.stargazer.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * src/test/resources/fixtures 아래의 파일을 읽는다.
 */
public final class Fixtures {

	private Fixtures() {
	}

	/**
	 * @param path fixtures 디렉터리 기준 경로. 예: "openweather/forecast-gangneung.json"
	 */
	public static String read(String path) {
		String resource = "/fixtures/" + path;
		try (InputStream in = Fixtures.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalArgumentException("fixture 파일이 없습니다: " + resource);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
