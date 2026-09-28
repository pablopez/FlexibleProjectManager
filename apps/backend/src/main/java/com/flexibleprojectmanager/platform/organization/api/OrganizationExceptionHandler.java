package com.flexibleprojectmanager.platform.organization.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.organization.application.OrganizationNotFoundException;
import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;

@RestControllerAdvice(assignableTypes = OrganizationController.class)
public class OrganizationExceptionHandler {
    @ExceptionHandler(OrganizationNotFoundException.class) public ResponseEntity<ErrorResponse> notFound(Exception e, WebRequest r) { return error(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Organization not found.", r); }
    @ExceptionHandler(AuthorizationException.class) public ResponseEntity<ErrorResponse> forbidden(Exception e, WebRequest r) { return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Organization permission is required.", r); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ErrorResponse> invalid(Exception e, WebRequest r) { return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The organization data is invalid.", r); }
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
