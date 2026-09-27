package com.flexibleprojectmanager.platform.users.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.flexibleprojectmanager.platform.authentication.api.AuthenticationDtos.ErrorResponse;
import com.flexibleprojectmanager.platform.users.application.UserNotFoundException;

@RestControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(UserNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found.", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> invalidInput(Exception exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The user data is invalid.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
