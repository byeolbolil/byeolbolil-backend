package xyz.ncookie.stargazer.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ObservationSpot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String title;           // 명소 이름 (예: 강릉 안반데기)

	@Column(nullable = false)
	private String address;         // 지번/도로명 주소 (네비게이션용)

	// --- 위치 정보 ---
	@Column(nullable = false)
	private Double latitude;        // 위도

	@Column(nullable = false)
	private Double longitude;       // 경도

	// --- 관측 관련 메타데이터 ---
	@Column(length = 1000)
	private String description;     // 소개글

	private Integer lightPollutionLevel; // 광해 등급 (Bortle Scale 1~9, 낮을수록 좋음) -> 수동 입력 시 유용

	// --- 편의 시설 ---
	private Boolean isParkingAvailable; 	// 주차 가능 여부
	private Boolean isRestroomAvailable; 	// 화장실 유무
	private Boolean isCarAccess;        	// 차량 진입 가능 여부 (차박 가능 여부)

	private String thumbnailImage;  // 썸네일 이미지 URL

	// --- 분류와 안내 (2026-11-25 명소 목록 재선정) ---
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private SpotKind kind;          // 열린 관측지인지 체험 시설인지

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private SpotStatus status;      // 뺀 곳은 RETIRED

	private String viewpoint;       // 실제로 차를 대고 보는 지점의 이름 (예: 멍에전망대 주차장). 좌표가 가리키는 곳이다

	@Column(length = 500)
	private String guide;           // 가는 법과 주차

	@Column(length = 500)
	private String caution;         // 계절 폐쇄, 조명, 통제 같은 주의 사항

	/** 컬럼을 더하기 전에 들어간 행은 값이 비어 있다. 비어 있으면 열린 관측지로 본다 */
	public SpotKind getKind() {
		return kind != null ? kind : SpotKind.OPEN_SITE;
	}

	public SpotStatus getStatus() {
		return status != null ? status : SpotStatus.ACTIVE;
	}

	public boolean isActive() {
		return getStatus() == SpotStatus.ACTIVE;
	}

	/** 밤 추천의 후보가 되는지. 지금 보여 주는 열린 관측지만 해당한다 */
	public boolean isRecommendable() {
		return isActive() && getKind() == SpotKind.OPEN_SITE;
	}

	public ObservationSpot(String title, Double latitude, Double longitude) {
		this.title = title;
		this.latitude = latitude;
		this.longitude = longitude;
	}
}
