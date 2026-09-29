package com.flexibleprojectmanager.platform.licensing.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.licensing.application.LicenseException;
import com.flexibleprojectmanager.platform.licensing.application.LicenseNotActiveException;
import com.flexibleprojectmanager.platform.licensing.application.LicenseNotFoundException;
import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;

@RestControllerAdvice
public class LicenseGlobalExceptionHandler {
    @ExceptionHandler(LicenseNotActiveException.class)
    public ResponseEntity<ErrorResponse> notActive(LicenseNotActiveException exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "LICENSE_NOT_ACTIVE", "An active license is required for this operation.", request);
    }

    @ExceptionHandler(LicenseException.class)
    public ResponseEntity<ErrorResponse> licenseFailure(LicenseException exception, WebRequest request) {
        HttpStatus status = "LICENSE_USER_LIMIT_EXCEEDED".equals(exception.code())
                ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        String message = "LICENSE_USER_LIMIT_EXCEEDED".equals(exception.code())
                ? "The license user limit has been exceeded." : "The license could not be used.";
        return error(status, exception.code(), message, request);
    }

    @ExceptionHandler(LicenseNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(LicenseNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "LICENSE_NOT_FOUND", "No license is installed.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
