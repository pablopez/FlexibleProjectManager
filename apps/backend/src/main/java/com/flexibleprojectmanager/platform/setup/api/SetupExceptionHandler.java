package com.flexibleprojectmanager.platform.setup.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.setup.application.SystemAlreadyInitializedException;
import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;

@RestControllerAdvice(assignableTypes = SetupController.class)
public class SetupExceptionHandler {
    @ExceptionHandler(SystemAlreadyInitializedException.class)
    public ResponseEntity<ErrorResponse> alreadyInitialized(SystemAlreadyInitializedException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "SYSTEM_ALREADY_INITIALIZED", "The system has already been initialized.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidInput(MethodArgumentNotValidException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The setup data is invalid.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception, WebRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "SETUP_FAILED", "The setup operation could not be completed.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest webRequest
                ? webRequest.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
