package xyz.ncookie.stargazer.domain.skylight.model;

/**
 * 한 좌표의 불빛 자료. 위성이 잰 야간 불빛에서 계산해 둔 값이다(scripts/lightpollution).
 *
 * @param skyBrightness  하늘 전체에서 평균한 인공 밝기가 자연 밤하늘 밝기의 몇 배인지(ALR).
 *                       Duriscoe et al., JQSRT 214, 133 (2018)의 모형으로 계산했다
 * @param nearbyRadiance 둘레 1km 안에서 위로 나가는 빛의 평균(nW/cm²/sr). 가로등과 건물 사이에 있는지를 가린다
 */
public record SkyLight(double skyBrightness, double nearbyRadiance) {
}
