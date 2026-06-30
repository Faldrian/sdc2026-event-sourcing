package com.seitenbau.sdc.sdc2026eventsourcing.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(AggregateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            AggregateNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex, request);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status, Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, ex.getMessage(), request));
    }
}
