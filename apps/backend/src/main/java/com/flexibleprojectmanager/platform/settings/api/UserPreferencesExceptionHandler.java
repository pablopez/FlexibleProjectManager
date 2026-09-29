package com.flexibleprojectmanager.platform.settings.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(assignableTypes = UserPreferencesController.class)
public class UserPreferencesExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> invalid(Exception exception, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                "VALIDATION_ERROR", "The user settings are invalid.", 400, Instant.now(), path));
    }
}
