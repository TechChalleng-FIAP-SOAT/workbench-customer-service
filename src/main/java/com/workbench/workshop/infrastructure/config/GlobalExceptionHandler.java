package com.workbench.workshop.infrastructure.config;

import com.workbench.workshop.application.user.BadCredentialsException;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({ClientNotFoundException.class, VehicleNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, 404, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String friendly = message != null && message.contains("duplicate key")
                ? "Registro duplicado: já existe um recurso com esse valor único."
                : "Violação de integridade dos dados.";
        return error(HttpStatus.CONFLICT, 409, friendly);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, 400, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, 401, ex.getMessage());
    }

    /** Cliente tentando ver cadastro/veículo de outro cliente. */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException ex) {
        return error(HttpStatus.FORBIDDEN, 403, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, 409, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(), "status", 400, "errors", errors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(), "status", 400, "errors", errors));
    }

    /** UUID/número inválido na URL (ex.: {@code /order/abc}) é erro do cliente, não 500/422. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error2(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' inválido: '" + ex.getValue() + "'.");
    }

    /** JSON mal formado ou valor fora do enum — sem repassar a mensagem do Jackson. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return error2(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido: confira o JSON e os tipos/valores dos campos.");
    }

    /**
     * Catch-all: não expõe detalhes internos (OWASP A05) — loga no servidor e responde
     * genérico. Exceções do próprio Spring MVC (405, 415, 404…) mantêm o status delas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse framework) {
            HttpStatus status = HttpStatus.valueOf(framework.getStatusCode().value());
            return error2(status, frameworkMessage(status));
        }
        log.error("Erro inesperado", ex);
        return error2(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.");
    }

    private static String frameworkMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado.";
            case METHOD_NOT_ALLOWED -> "Método HTTP não suportado nesta rota.";
            case UNSUPPORTED_MEDIA_TYPE -> "Content-Type não suportado; use application/json.";
            case NOT_ACCEPTABLE -> "Formato de resposta não suportado.";
            default -> "Requisição inválida.";
        };
    }

    private ResponseEntity<Map<String, Object>> error2(HttpStatus status, String message) {
        return error(status, status.value(), message);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", code,
                "error", message == null ? "" : message));
    }
}
