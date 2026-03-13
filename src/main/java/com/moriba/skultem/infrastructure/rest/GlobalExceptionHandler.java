package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.rest.dto.ValidationError;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
                return build(HttpStatus.NOT_FOUND, ex.getMessage());
        }

        @ExceptionHandler(AlreadyExistsException.class)
        public ResponseEntity<Object> handleAlreadyExists(AlreadyExistsException ex) {
                return build(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(RuleException.class)
        public ResponseEntity<Object> handleRule(RuleException ex) {
                return build(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
                return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
                return build(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Object> handleGeneralException(Exception ex) {
                log.error("Unhandled exception: {}", ex.getMessage(), ex);

                return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ValidationError> handleValidation(MethodArgumentNotValidException ex) {

                Map<String, String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                FieldError::getDefaultMessage,
                                                (msg1, msg2) -> msg1));

                ValidationError response = new ValidationError(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation failed",
                                LocalDateTime.now(),
                                errors);

                return ResponseEntity.badRequest().body(response);
        }

        private ResponseEntity<Object> build(HttpStatus status, String message) {
                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("status", status.value());
                body.put("message", message);
                return new ResponseEntity<>(body, status);
        }
}