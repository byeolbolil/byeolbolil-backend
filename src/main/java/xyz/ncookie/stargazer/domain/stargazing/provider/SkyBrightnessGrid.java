package xyz.ncookie.stargazer.domain.stargazing.provider;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.model.SkyLight;

/**
 * 좌표로 하늘 밝기와 둘레의 불빛을 찾는다.
 *
 * 값은 scripts/lightpollution이 만든 격자 파일에 있고, 기동할 때 한 번 메모리에 올린다. 한국 둘레 30초각(약
 * 0.9km) 격자 54만 칸이고 2MB쯤이다. DB를 거치지 않으므로 적재를 기다릴 일도, 캐시할 일도 없다.
 * 파일 형식은 scripts/lightpollution/export_grid.py에 적혀 있다.
 */
@Slf4j
@Component
public class SkyBrightnessGrid {

	static final String RESOURCE = "skyglow/skyglow_korea.bin.gz";

	private static final String MAGIC = "SKYG";
	private static final int VERSION = 1;

	private final int rows;
	private final int cols;
	private final double north;
	private final double west;
	private final double step;
	// round(1000 * log10(값))으로 적힌 두 층. 줄마다 서쪽에서 동쪽으로, 북쪽 줄부터
	private final short[] skyBrightness;
	private final short[] nearbyRadiance;

	public SkyBrightnessGrid() {
		this(new ClassPathResource(RESOURCE));
	}

	SkyBrightnessGrid(ClassPathResource resource) {
		try (InputStream raw = resource.getInputStream();
			 DataInputStream in = new DataInputStream(new GZIPInputStream(raw, 1 << 16))) {
			byte[] magic = in.readNBytes(4);
			if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
				throw new IllegalStateException("하늘 밝기 격자 파일이 아닙니다: " + resource.getPath());
			}
			int version = in.readInt();
			if (version != VERSION) {
				throw new IllegalStateException("읽을 수 없는 격자 파일 버전입니다: " + version);
			}
			this.rows = in.readInt();
			this.cols = in.readInt();
			this.north = in.readDouble();
			this.west = in.readDouble();
			this.step = in.readDouble();
			this.skyBrightness = readLayer(in, rows * cols);
			this.nearbyRadiance = readLayer(in, rows * cols);
		} catch (IOException e) {
			throw new UncheckedIOException("하늘 밝기 격자를 읽지 못했습니다: " + resource.getPath(), e);
		}
		log.info("하늘 밝기 격자 {} x {} 칸을 읽었습니다.", rows, cols);
	}

	private static short[] readLayer(DataInputStream in, int length) throws IOException {
		short[] layer = new short[length];
		for (int i = 0; i < length; i++) {
			layer[i] = in.readShort();
		}
		return layer;
	}

	/**
	 * @return 격자 밖의 좌표면 비어 있다
	 */
	public Optional<SkyLight> lookup(double latitude, double longitude) {
		// 칸의 중심을 격자점으로 보고 둘레 네 점 사이를 보간한다
		double y = (north - latitude) / step - 0.5;
		double x = (longitude - west) / step - 0.5;
		if (y < 0 || x < 0 || y > rows - 1 || x > cols - 1) {
			return Optional.empty();
		}
		int row = Math.min((int) y, rows - 2);
		int col = Math.min((int) x, cols - 2);
		double fy = y - row;
		double fx = x - col;

		return Optional.of(new SkyLight(
			interpolate(skyBrightness, row, col, fy, fx),
			interpolate(nearbyRadiance, row, col, fy, fx)));
	}

	/** 로그값을 쌍선형으로 보간한 뒤 원래 값으로 되돌린다 */
	private double interpolate(short[] layer, int row, int col, double fy, double fx) {
		int index = row * cols + col;
		double top = layer[index] * (1 - fx) + layer[index + 1] * fx;
		double bottom = layer[index + cols] * (1 - fx) + layer[index + cols + 1] * fx;
		return Math.pow(10, (top * (1 - fy) + bottom * fy) / 1000.0);
	}
}
