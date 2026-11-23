package xyz.ncookie.stargazer.domain.recommend.engine;

/**
 * 달의 위상각으로 보름달 대비 밝기를 구한다.
 *
 * 달빛은 빛나는 면적에 비례하지 않는다. 반달은 면적이 절반이지만 밝기는 보름달의 9%쯤이다.
 *
 * 출처: Krisciunas & Schaefer, "A Model of the Brightness of Moonlight", PASP 103, 1033 (1991),
 * https://doi.org/10.1086/132921 의 대기 밖 달 조도 식
 *   I* = 10^(-0.4 (3.84 + 0.026|α| + 4×10⁻⁹ α⁴))   (α: 위상각, 도 단위, 보름일 때 0)
 * 원 논문은 스캔본이라 본문을 직접 읽지 못했다. 위상에 따라 달라지는 항(0.026|α| + 4.0×10⁻⁹α⁴)은
 * Noll et al., "An atmospheric radiation model for Cerro Paranal", arXiv:1205.2003 의 식 (15)에서 확인했다.
 * 상수 3.84는 보름달과의 비를 구하면 약분되어 여기서는 쓰이지 않는다.
 */
public final class MoonBrightness {

	private static final double LINEAR_TERM = 0.026;
	private static final double QUARTIC_TERM = 4.0e-9;

	private MoonBrightness() {
	}

	/**
	 * @param phaseAngleDegrees 위상각. 보름일 때 0, 삭일 때 180. 부호는 무시한다
	 * @return 보름달을 1로 본 밝기
	 */
	public static double relativeToFull(double phaseAngleDegrees) {
		double alpha = Math.min(Math.abs(phaseAngleDegrees), 180.0);
		return Math.pow(10, -0.4 * (LINEAR_TERM * alpha + QUARTIC_TERM * Math.pow(alpha, 4)));
	}
}
