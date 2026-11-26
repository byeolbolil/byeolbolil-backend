package xyz.ncookie.stargazer.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그 명소에서 별을 본 인스타그램 게시물 하나. 인스타그램은 제목과 미리보기 그림을 내주지 않아서, 화면은 인스타그램이
 * 제공하는 임베드 주소로 게시물을 그대로 보여 준다. 한 줄 설명은 우리가 적은 것이고 계정 이름은 게시물에 보이는 그대로다.
 * data.sql이 원본이라 코드에서는 읽기만 한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotReel {

	@Column(nullable = false, length = 40)
	private String postId;      // 게시물 주소의 /reel/ 뒤에 오는 값

	@Column(nullable = false, length = 100)
	private String title;       // 어떤 영상인지 우리가 적은 한 줄

	@Column(nullable = false, length = 60)
	private String account;     // 올린 계정의 이름

	/** 게시물을 인스타그램에서 여는 주소 */
	public String getUrl() {
		return "https://www.instagram.com/reel/" + postId + "/";
	}

	/** 게시물을 다른 사이트 안에 넣어 보여 주는 인스타그램의 임베드 주소. 로그인 없이 열린다 */
	public String getEmbedUrl() {
		return getUrl() + "embed/";
	}
}
