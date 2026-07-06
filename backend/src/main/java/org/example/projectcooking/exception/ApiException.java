package org.example.projectcooking.exception;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Базовое доменное исключение бэкенда. Несёт HTTP-статус, машиночитаемый {@code code}
 * (см. openapi {@code Error.code}) и опциональный {@code details} — переводится
 * {@code GlobalExceptionHandler} в тело {@code Error}/{@code BlockedError}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient Map<String, Object> details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException conflict(String code, String message, Map<String, Object> details) {
        return new ApiException(HttpStatus.CONFLICT, code, message, details);
    }

    public static ApiException gone(String code, String message, Map<String, Object> details) {
        return new ApiException(HttpStatus.GONE, code, message, details);
    }

    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
