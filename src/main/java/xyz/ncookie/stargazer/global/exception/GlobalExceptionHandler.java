package xyz.ncookie.stargazer.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.dto.CommonResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<CommonResponse<Void>> handleBaseException(BaseException e) {

		ErrorCode code = e.getErrorCode();

		return ResponseEntity
			.status(code.getStatus())
			.body(CommonResponse.error(code));
	}

	/**
	 * [GET] @ModelAttribute 유효성 검사 실패 시 발생 (BindException)
	 * [POST] @RequestBody 유효성 검사 실패 시 발생 (MethodArgumentNotValidException)
	 * 두 예외를 모두 잡아서 처리
	 * MethodArgumentNotValidException은 BindException을 상속받으므로 BindException으로 한 번에 처리가 가능
	 */
	@ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
	public ResponseEntity<CommonResponse<Map<String, String>>> handleValidationException(BindException e) {

		BindingResult bindingResult = e.getBindingResult();
		Map<String, String> errorMap = new HashMap<>();

		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			String message;

			// 변환(TypeMismatch) 실패인지 체크
			if (fieldError.isBindingFailure()) {
				message = "입력 형식이 올바르지 않습니다."; // "Failed to convert..." 대신 나갈 메시지
			} else {
				message = fieldError.getDefaultMessage(); // @Annotation 메시지
			}

			errorMap.put(fieldError.getField(), message);
		}

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.error(HttpStatus.BAD_REQUEST.value(), "유효성 검사에 실패하였습니다.", errorMap));
	}

	/**
	 * 경로 변수나 쿼리 파라미터를 선언한 타입으로 바꾸지 못했을 때 (예: 날짜 자리에 날짜가 아닌 값)
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<CommonResponse<Map<String, String>>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.error(
				HttpStatus.BAD_REQUEST.value(),
				"유효성 검사에 실패하였습니다.",
				Map.of(e.getName(), "입력 형식이 올바르지 않습니다.")));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CommonResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {

		log.debug("잘못된 요청 인자: ", e);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleGeneralException(Exception e) {

		// 없는 경로, 지원하지 않는 메서드처럼 Spring이 상태 코드를 정해 둔 예외는 서버 오류가 아니다.
		// 500으로 답하면 스캐너가 /를 찌를 때마다 오류 로그와 스택 트레이스가 쌓인다.
		if (e instanceof ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
			HttpStatusCode status = errorResponse.getStatusCode();
			String message = status.value() == HttpStatus.NOT_FOUND.value()
				? "요청한 경로를 찾을 수 없습니다."
				: errorResponse.getBody().getDetail();

			return ResponseEntity
				.status(status)
				.body(CommonResponse.error(status.value(), message, null));
		}

		log.error("예상하지 못한 예외 발생: ", e);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null));
	}
}
