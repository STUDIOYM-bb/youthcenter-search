package com.themoa.youthcentersearch.common.exception;

import com.themoa.youthcentersearch.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
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
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.fail("?몃? API ?곌껐 以??ㅽ듃?뚰겕 ?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("泥섎━ 以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎."));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + (error.getDefaultMessage() == null ? "?섎せ??媛믪엯?덈떎." : error.getDefaultMessage());
    }
}
