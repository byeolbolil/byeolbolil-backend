package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Instant;

/**
 * 밤의 한 시간 칸(time부터 1시간)의 조건.
 *
 * @param time               칸이 시작하는 시각(정시)
 * @param dark               칸의 처음부터 끝까지 태양이 -12° 아래에 있는지
 * @param sunAltitude        칸 안에서 태양이 가장 높을 때의 고도 (°)
 * @param cloudCover         총운량 (%)
 * @param cloudCoverLow      하층운량 (%)
 * @param cloudCoverMid      중층운량 (%)
 * @param cloudCoverHigh     상층운량 (%)
 * @param relativeHumidity   상대습도 (%)
 * @param moonUp             칸 한가운데 시각에 달이 지평선 위에 있는지
 * @param moonBrightness     보름달을 1로 본 달의 밝기. 떠 있는지와 상관없이 위상만으로 정한 값이다
 * @param clear              어둡고 구름이 관측 가능 기준 이하인지
 * @param score              0~100. 어둡지 않은 칸은 0이다
 */
public record NightHour(
	Instant time,
	boolean dark,
	double sunAltitude,
	int cloudCover,
	int cloudCoverLow,
	int cloudCoverMid,
	int cloudCoverHigh,
	int relativeHumidity,
	boolean moonUp,
	double moonBrightness,
	boolean clear,
	int score
) {}
