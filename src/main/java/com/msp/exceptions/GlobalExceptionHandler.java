package com.msp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRegistrationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRegistrationException(
            BusinessRegistrationException ex) {

        String message = ex.getMessage();

        // Determine the right status from the message content
        HttpStatus status;
        if (message != null && (message.contains("already exists") || message.contains("already registered"))) {
            status = HttpStatus.CONFLICT;                   // 409
        } else if (message != null && message.contains("Cannot transition")) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;       // 422
        } else if (message != null && message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;                  // 404
        } else {
            status = HttpStatus.BAD_REQUEST;                // 400
        }   

        return ResponseEntity.status(status).body(errorBody(status, message));
    }

    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<Map<String, Object>> handleCustomerException(CustomerException ex) {
        String message = ex.getMessage();
        HttpStatus status;
        if (message != null && (message.contains("already registered")
                || message.contains("already exists")
                || message.contains("already in use"))) {
            status = HttpStatus.CONFLICT;                   // 409
        } else if (message != null && (message.contains("not found")
                || message.contains("not interacted"))) {
            status = HttpStatus.NOT_FOUND;                  // 404
        } else {
            status = HttpStatus.BAD_REQUEST;                // 400
        }
        return ResponseEntity.status(status).body(errorBody(status, message));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(UserException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(errorBody(status, ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

    @ExceptionHandler(PortalException.class)
    public ResponseEntity<Map<String, Object>> handlePortalException(PortalException ex) {
        String message = ex.getMessage();
        HttpStatus status;
        if (message != null && message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message != null && (message.contains("already exists")
                || message.contains("already in use"))) {
            status = HttpStatus.CONFLICT;
        } else if (message != null && (message.contains("not authorized")
                || message.contains("does not belong"))) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(errorBody(status, message));
    }


    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
