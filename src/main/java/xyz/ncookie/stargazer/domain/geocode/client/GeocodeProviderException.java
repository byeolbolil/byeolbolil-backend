package xyz.ncookie.stargazer.domain.geocode.client;

/**
 * 지명 검색 소스가 답을 주지 못했다는 뜻이다. 호출 실패, 시간 초과, 쓸 수 없는 응답을 모두 이 예외로 모은다.
 * 도메인 서비스가 잡아서 GeocodeException으로 바꾸고, 컨트롤러 밖으로는 나가지 않는다.
 */
public class GeocodeProviderException extends RuntimeException {

	public GeocodeProviderException(String message) {
		super(message);
	}

	public GeocodeProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
