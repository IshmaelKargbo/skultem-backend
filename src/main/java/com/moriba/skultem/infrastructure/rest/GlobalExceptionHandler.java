package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.FileUploadException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.rest.dto.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(AlreadyExistsException ex) {
        return build(HttpStatus.BAD_REQUEST, "ALREADY_EXISTS", ex);
    }

    @ExceptionHandler(RuleException.class)
    public ResponseEntity<ApiErrorResponse> handleRule(RuleException ex) {
        return build(HttpStatus.BAD_REQUEST, "RULE_VIOLATION", ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "ACCESS_DENIED", ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", ex);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleFileUpload(FileUploadException ex) {
        log.warn("File upload failed: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "FILE_UPLOAD_FAILED",
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, Object> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a));

        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.put(globalError.getObjectName(), globalError.getDefaultMessage());
        }

        log.warn("Validation failed: {}", errors);

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_FAILED",
                        "Validation error",
                        LocalDateTime.now(),
                        errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR",
                        "Something went wrong. Please try again later.",
                        LocalDateTime.now(),
                        null));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String errorCode, Exception ex) {
        log.error("{}: {}", errorCode, ex.getMessage(), ex);

        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        status.value(),
                        errorCode,
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null));
    }
}
