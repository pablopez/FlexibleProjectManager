package com.flexibleprojectmanager.platform.audit.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;

@RestControllerAdvice(assignableTypes = AuditController.class)
public class AuditExceptionHandler {
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> forbidden(Exception e, WebRequest r) { return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Audit read permission is required.", r); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> invalid(Exception e, WebRequest r) { return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The audit query is invalid.", r); }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> invalidParameter(MethodArgumentTypeMismatchException e, WebRequest r) { return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The audit query is invalid.", r); }
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
