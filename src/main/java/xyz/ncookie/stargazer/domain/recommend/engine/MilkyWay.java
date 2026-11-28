package xyz.ncookie.stargazer.domain.recommend.engine;

/**
 * 은하수의 밝은 중심부를 대표하는 점. 은하 중심의 전파원 궁수자리 A*의 위치로 잡는다.
 *
 * 은하수 전체의 띠를 계산하지 않고 중심 한 점의 고도만 본다. 겨울 은하수는 옅어서 입문자가 기대하는 모습이 아니다
 * (docs/decisions.md 2026-11-27 "은하수가 보이는 시간과 천문 현상").
 */
public final class MilkyWay {

	/**
	 * 궁수자리 A*의 J2000(ICRF3) 적경 17h45m40.034047s를 도로 바꾼 값.
	 * Gordon, de Witt & Jacobs (2023), https://arxiv.org/abs/2212.00632 초록. 2015.0 고유운동 기준의 값이고,
	 * 고유운동은 연 수 mas라 이 계산에는 상관없다.
	 */
	public static final double CENTER_RA = (17 + 45 / 60.0 + 40.034047 / 3600.0) * 15;

	/** 같은 자료의 적위 -29°00′28.21601″ */
	public static final double CENTER_DEC = -(29 + 0 / 60.0 + 28.21601 / 3600.0);

	private MilkyWay() {
	}
}
