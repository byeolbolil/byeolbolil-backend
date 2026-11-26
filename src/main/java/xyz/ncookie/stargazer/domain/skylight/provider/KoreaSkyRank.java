package xyz.ncookie.stargazer.domain.skylight.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 하늘 밝기가 남한 땅에서 몇 번째로 어두운 편인지. 남한 땅을 하늘이 어두운 순서로 백 칸으로 나눈 표로 찾는다.
 *
 * 하늘 밝기 등급은 미국 국립공원 기준이라 한국 본토는 거의 아래쪽 두 칸에 몰린다. 같은 등급 안에서 국내에서 얼마나
 * 어두운 편인지를 따로 말하려고 둔다. 표는 scripts/lightpollution/korea_rank.py가 하늘 밝기 격자와 고도 자료로 만든다.
 */
@Component
public class KoreaSkyRank {

	static final String RESOURCE = "skyglow/korea_rank.csv";
	private static final int SIZE = 100;

	/** 칸 k(1~100)의 하늘 밝기 상한. 남한 땅의 k%가 이 값 이하다 */
	private final double[] upperBounds = new double[SIZE];

	public KoreaSkyRank() {
		this(new ClassPathResource(RESOURCE));
	}

	KoreaSkyRank(ClassPathResource resource) {
		int read = 0;
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split(",");
				upperBounds[Integer.parseInt(parts[0].trim()) - 1] = Double.parseDouble(parts[1].trim());
				read++;
			}
		} catch (IOException e) {
			throw new UncheckedIOException("국내 하늘 밝기 순위표를 읽지 못했습니다: " + resource.getPath(), e);
		}
		if (read != SIZE) {
			throw new IllegalStateException("국내 하늘 밝기 순위표는 " + SIZE + "줄이어야 합니다: " + read);
		}
	}

	/**
	 * @return 1~100. 1이면 남한 땅에서 가장 어두운 1% 안이고, 100이면 가장 밝은 1% 안이다
	 */
	public int rankOf(double skyBrightness) {
		for (int k = 0; k < SIZE; k++) {
			if (skyBrightness <= upperBounds[k]) {
				return k + 1;
			}
		}
		return SIZE;
	}
}
