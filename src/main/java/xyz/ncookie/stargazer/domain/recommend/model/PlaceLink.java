package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 명소의 참고 링크 하나.
 *
 * @param title    그 글의 제목
 * @param url      주소
 * @param imageUrl 그 글의 대표 이미지. 미리보기로 쓴다. 없으면 null
 */
public record PlaceLink(String title, String url, String imageUrl) {}
