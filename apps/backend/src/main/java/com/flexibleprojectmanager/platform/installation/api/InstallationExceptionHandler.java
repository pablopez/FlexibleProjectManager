package com.flexibleprojectmanager.platform.installation.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import com.flexibleprojectmanager.platform.installation.application.InstallationInvariantException;
import com.flexibleprojectmanager.platform.installation.application.InstallationNotFoundException;
import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;

@RestControllerAdvice(assignableTypes = InstallationController.class)
public class InstallationExceptionHandler {
    @ExceptionHandler(InstallationNotFoundException.class) public ResponseEntity<ErrorResponse> notFound(Exception e, WebRequest r) { return error(HttpStatus.NOT_FOUND, "INSTALLATION_NOT_FOUND", "Installation not found.", r); }
    @ExceptionHandler(InstallationInvariantException.class) public ResponseEntity<ErrorResponse> invariant(Exception e, WebRequest r) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "INSTALLATION_INVARIANT_BROKEN", "The local installation invariant is unavailable.", r); }
    @ExceptionHandler(AuthorizationException.class) public ResponseEntity<ErrorResponse> forbidden(Exception e, WebRequest r) { return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Installation permission is required.", r); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ErrorResponse> invalid(Exception e, WebRequest r) { return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The installation data is invalid.", r); }
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) { String path=request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : ""; return ResponseEntity.status(status).body(new ErrorResponse(code,message,status.value(),Instant.now(),path)); }
}
