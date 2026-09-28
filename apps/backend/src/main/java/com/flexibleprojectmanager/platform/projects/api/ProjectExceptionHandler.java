package com.flexibleprojectmanager.platform.projects.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;
import com.flexibleprojectmanager.platform.projects.application.ProjectNotFoundException;
import com.flexibleprojectmanager.platform.projects.application.ProjectAlreadyActiveException;
import com.flexibleprojectmanager.platform.projects.application.ProjectAlreadyArchivedException;

@RestControllerAdvice(assignableTypes = ProjectController.class)
public class ProjectExceptionHandler {
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ProjectNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found.", request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> forbidden(AuthorizationException exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Project permission is required.", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> invalidInput(Exception exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The project data is invalid.", request);
    }

    @ExceptionHandler(ProjectAlreadyArchivedException.class)
    public ResponseEntity<ErrorResponse> alreadyArchived(ProjectAlreadyArchivedException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "PROJECT_ALREADY_ARCHIVED", "Project is already archived.", request);
    }

    @ExceptionHandler(ProjectAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> alreadyActive(ProjectAlreadyActiveException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "PROJECT_ALREADY_ACTIVE", "Project is already active.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
