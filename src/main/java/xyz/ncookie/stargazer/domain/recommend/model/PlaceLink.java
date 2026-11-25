package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 명소의 참고 링크 하나.
 *
 * @param title 그 글의 제목
 * @param url   주소
 */
public record PlaceLink(String title, String url) {}
