package com.flexibleprojectmanager.platform.licensing.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.licensing.application.LicenseNotFoundException;
import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;

@RestControllerAdvice(assignableTypes = LicenseController.class)
public class LicenseExceptionHandler {
    @ExceptionHandler(LicenseNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(LicenseNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "LICENSE_NOT_FOUND", "No license is installed.", request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> forbidden(AuthorizationException exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "License permission is required.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> invalid(IllegalArgumentException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "LICENSE_INVALID", "The license request is invalid.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
