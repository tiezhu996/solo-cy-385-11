package com.babytracker.exception;

import com.babytracker.constants.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException ex) {
        return ResponseEntity.status(statusOf(ex.getCode()))
                .body(Map.of("success", false, "code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "code", ErrorCode.VALIDATION_FAILED, "message", "请求参数不合法"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception ex) {
        log.error("未处理的服务器异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "code", ErrorCode.INTERNAL_ERROR, "message", "服务器内部错误"));
    }

    private HttpStatus statusOf(String code) {
        return switch (code) {
            case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.ALREADY_MEMBER, ErrorCode.INVITE_INVALID, ErrorCode.INVITE_EXPIRED,
                 ErrorCode.INVITE_REVOKED, ErrorCode.INVITE_CLAIMED, ErrorCode.CREATOR_FORBIDDEN,
                 ErrorCode.LAST_MANAGER_REQUIRED, ErrorCode.NICKNAME_TAKEN -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
