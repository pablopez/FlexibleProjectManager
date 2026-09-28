package com.flexibleprojectmanager.platform.users.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.flexibleprojectmanager.platform.shared.api.ErrorResponse;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;
import com.flexibleprojectmanager.platform.users.application.InvalidUserRoleException;
import com.flexibleprojectmanager.platform.users.application.LastActiveAdminRequiredException;
import com.flexibleprojectmanager.platform.users.application.UserEmailAlreadyExistsException;
import com.flexibleprojectmanager.platform.users.application.UserNotFoundException;

@RestControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(UserNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found.", request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> forbidden(AuthorizationException exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not authorized to perform this operation.", request);
    }

    @ExceptionHandler(UserEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> duplicate(UserEmailAlreadyExistsException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "USER_EMAIL_ALREADY_EXISTS", "The email address is already in use.", request);
    }

    @ExceptionHandler(LastActiveAdminRequiredException.class)
    public ResponseEntity<ErrorResponse> lastAdmin(LastActiveAdminRequiredException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "LAST_ACTIVE_ADMIN_REQUIRED", "The organization must retain an active administrator.", request);
    }

    @ExceptionHandler(InvalidUserRoleException.class)
    public ResponseEntity<ErrorResponse> invalidRole(InvalidUserRoleException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_USER_ROLE", "The requested role is not a valid system role.", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> invalidInput(Exception exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The user data is invalid.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, status.value(), Instant.now(), path));
    }
}
