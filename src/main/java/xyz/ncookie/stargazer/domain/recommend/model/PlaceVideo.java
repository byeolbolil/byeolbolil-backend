package xyz.ncookie.stargazer.domain.recommend.model;

/**
 * 그 명소에서 별을 본 유튜브 영상 하나.
 *
 * @param title        영상 제목
 * @param channel      올린 채널의 이름
 * @param url          영상을 여는 주소
 * @param thumbnailUrl 미리보기 그림의 주소
 * @param shortForm    쇼츠인지
 */
public record PlaceVideo(String title, String channel, String url, String thumbnailUrl, boolean shortForm) {}
