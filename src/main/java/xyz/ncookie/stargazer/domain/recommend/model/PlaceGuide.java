package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 명소에 붙은 안내. 평가에는 쓰지 않고 화면에 보여 주기만 한다. 값마다 없을 수 있다.
 *
 * @param viewpoint   실제로 차를 대고 보는 지점의 이름
 * @param description 어떤 곳인지
 * @param guide       가는 법과 주차
 * @param caution     계절 폐쇄, 조명, 통제 같은 주의 사항
 */
public record PlaceGuide(String viewpoint, String description, String guide, String caution) {}
