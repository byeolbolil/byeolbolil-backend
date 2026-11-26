package xyz.ncookie.stargazer.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그 명소에서 별을 본 유튜브 영상 하나. 가기 전에 어떤 곳인지 눈으로 보라고 고른 남의 영상이고, 제목과 채널 이름은
 * 유튜브가 알려 준 그대로다. scripts/spots/videos.py가 고른 목록(videos.tsv)에서 data.sql의 행을 만든다.
 * data.sql이 원본이라 코드에서는 읽기만 한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotVideo {

	@Column(nullable = false, length = 20)
	private String videoId;     // 유튜브 영상 ID (주소의 v= 뒤)

	@Column(nullable = false, length = 200)
	private String title;       // 영상 제목

	@Column(nullable = false, length = 100)
	private String channel;     // 올린 채널의 이름

	@Column(nullable = false)
	private boolean shortForm;  // 쇼츠(세로로 찍은 짧은 영상)인지

	/** 영상을 여는 주소. 쇼츠는 쇼츠 화면으로 연다 */
	public String getUrl() {
		return shortForm
			? "https://www.youtube.com/shorts/" + videoId
			: "https://www.youtube.com/watch?v=" + videoId;
	}

	/**
	 * 미리보기 그림의 주소. 유튜브가 영상마다 같은 자리에 두는 320×180 그림이다.
	 * 영상이 지워지면 이 주소는 120×90의 빈 그림을 돌려준다.
	 */
	public String getThumbnailUrl() {
		return "https://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
	}
}
