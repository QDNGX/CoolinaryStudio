package org.example.projectcooking.web;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.example.projectcooking.dto.error.BlockedErrorResponse;
import org.example.projectcooking.dto.error.ErrorResponse;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.exception.ClientBlockedException;
import org.example.projectcooking.exception.RateLimitException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Единая обработка ошибок → тело {@code Error}/{@code BlockedError} по контракту openapi.
 * 401/403 (нет/невалиден токен, роль не подходит) обрабатывает Spring Security, не этот advice.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 403 с датой разблокировки (D-02). */
    @ExceptionHandler(ClientBlockedException.class)
    public ResponseEntity<BlockedErrorResponse> handleBlocked(ClientBlockedException ex) {
        return ResponseEntity.status(ex.getStatus()).body(BlockedErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .blockedUntil(ex.getBlockedUntil())
                .build());
    }

    /** 429 с Retry-After (НФТ-04). */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(toError(ex));
    }

    /** Доменные исключения (400/404/409/410/422). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(toError(ex));
    }

    /**
     * Отказ по роли из @PreAuthorize (аутентифицированный, но недостаточно прав) → 403.
     * Обрабатывается здесь, т.к. исключение метод-безопасности летит через DispatcherServlet;
     * без токена запрос отсекается раньше (URL-правило authenticated → 401 в SecurityConfig).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .code("FORBIDDEN")
                .message("Операция недоступна текущей роли")
                .build());
    }

    /** Конфликт оптимистичной блокировки — параллельная бронь успела раньше (D-03). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .code("SLOT_JUST_TAKEN")
                .message("Место только что заняли")
                .build());
    }

    /** Нарушение целостности (например, гонка на unique-индексе отзыва D-05). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Нарушение целостности данных: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .code("CONFLICT")
                .message("Конфликт данных — повторите операцию")
                .build());
    }

    /** Ошибки bean-валидации тела запроса. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> details.put(ge.getObjectName(), ge.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Некорректный запрос")
                .details(details)
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message(ex.getMessage())
                .build());
    }

    /** Некорректный тип параметра пути/квери (например, невалидный UUID или значение enum). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("BAD_REQUEST")
                .message("Некорректное значение параметра «" + ex.getName() + "»")
                .build());
    }

    /** Нечитаемое/битое тело запроса. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("MALFORMED_REQUEST")
                .message("Тело запроса не удалось прочитать")
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Необработанная ошибка", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("Внутренняя ошибка сервера")
                .build());
    }

    private ErrorResponse toError(ApiException ex) {
        return ErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .build();
    }
}
