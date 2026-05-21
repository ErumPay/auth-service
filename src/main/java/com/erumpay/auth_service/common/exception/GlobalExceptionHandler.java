package com.erumpay.auth_service.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", e.getStatus().value());
        body.put("message", e.getMessage());
        if (e.getExtra() != null) {
            body.put("extra", e.getExtra());
        }
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("유효하지 않은 요청");
        Map<String, Object> body = Map.of("status", 400, "message", message);
        return ResponseEntity.badRequest().body(body);
    }
}
