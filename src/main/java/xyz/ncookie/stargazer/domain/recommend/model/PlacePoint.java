package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 명소 안에서 서서 보는 자리 하나.
 *
 * @param name      자리의 이름
 * @param access    어떻게 가는지와 어떤 자리인지
 * @param latitude  좌표를 모르면 null
 * @param longitude 좌표를 모르면 null
 */
public record PlacePoint(String name, String access, Double latitude, Double longitude) {}
