package xyz.ncookie.stargazer.domain.weather.client;

/**
 * 예보 소스 하나가 답을 주지 못했다는 뜻이다. 호출 실패, 시간 초과, 쓸 수 없는 응답을 모두 이 예외로 모은다.
 * 도메인 서비스가 잡아서 보조 소스로 넘어가는 데 쓰고, 컨트롤러 밖으로는 나가지 않는다.
 */
public class WeatherProviderException extends RuntimeException {

	public WeatherProviderException(String message) {
		super(message);
	}

	public WeatherProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
