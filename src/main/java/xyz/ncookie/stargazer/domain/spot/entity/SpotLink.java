package xyz.ncookie.stargazer.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 명소의 참고 링크 하나. 가는 길과 서는 자리를 더 자세히 적은 기사, 관광 안내, 후기이고 시설은 공식 페이지다.
 * data.sql이 원본이라 코드에서는 읽기만 한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotLink {

	@Column(nullable = false, length = 100)
	private String title;   // 그 글의 제목

	@Column(nullable = false, length = 300)
	private String url;
}
