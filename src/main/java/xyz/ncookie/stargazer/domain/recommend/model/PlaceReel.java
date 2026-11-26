package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 그 명소에서 별을 본 인스타그램 게시물 하나.
 *
 * @param title    어떤 영상인지 적은 한 줄
 * @param account  올린 계정의 이름
 * @param url      인스타그램에서 여는 주소
 * @param embedUrl 화면 안에 넣어 보여 주는 임베드 주소
 */
public record PlaceReel(String title, String account, String url, String embedUrl) {}
