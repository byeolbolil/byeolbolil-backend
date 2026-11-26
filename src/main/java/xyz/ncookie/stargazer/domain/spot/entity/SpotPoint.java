package xyz.ncookie.stargazer.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 명소 안에서 실제로 서서 보는 자리 하나. 산이나 고원 하나에 자리가 여럿인 곳이 많다(황매산의 주차장과 별빛언덕).
 * 첫 자리는 차를 대는 곳이고, 나머지는 거기서 차로 조금 가거나 30분 안쪽으로 걸어가는 곳이다.
 * data.sql이 원본이라 코드에서는 읽기만 한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotPoint {

	@Column(nullable = false, length = 60)
	private String name;    // 자리의 이름 (예: 별빛언덕)

	@Column(nullable = false, length = 300)
	private String access;  // 어떻게 가는지와 어떤 자리인지 (예: 주차장에서 걸어서 20분쯤)

	// 좌표를 아는 자리에만 있다
	private Double latitude;
	private Double longitude;
}
