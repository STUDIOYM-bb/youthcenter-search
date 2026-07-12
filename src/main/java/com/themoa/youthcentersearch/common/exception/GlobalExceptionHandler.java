package com.themoa.youthcentersearch.common.exception;

import com.themoa.youthcentersearch.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(YouthCenterApiResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiResponse(YouthCenterApiResponseException ex) {
        log.warn("YouthCenter response error: status={}, type={}, url={}, message={}",
                ex.getStatusCode(), ex.getResponseType(), ex.getMaskedRequestUrl(), ex.getMessage());
        HttpStatus status = ex.getStatusCode() >= 500 ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(YouthCenterApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(YouthCenterApiException ex) {
        log.warn("YouthCenter API error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler({SocketTimeoutException.class, UnknownHostException.class})
    public ResponseEntity<ApiResponse<Void>> handleNetwork(Exception ex) {
        log.warn("Network error: {}", ex.toString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.fail("외부 API 연결에 실패했습니다."));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(readableMessage(ex.getMessage())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("서버 처리 중 오류가 발생했습니다."));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + (error.getDefaultMessage() == null ? "입력값이 올바르지 않습니다." : error.getDefaultMessage());
    }

    private String readableMessage(String message) {
        if ("JOB_ALREADY_RUNNING".equals(message)) {
            return "같은 종류의 작업이 이미 실행 중입니다.";
        }
        return message == null || message.isBlank() ? "요청을 처리할 수 없습니다." : message;
    }
}
