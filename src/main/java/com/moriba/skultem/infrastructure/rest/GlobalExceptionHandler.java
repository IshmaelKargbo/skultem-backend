package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.BadRequestException;
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

    // Was missing entirely - BadRequestException (geofence/IP checks on clock-in/out, payroll
    // run and leave request validation, user creation, etc.) fell through to the generic
    // Exception handler below, so its specific, actionable message (e.g. "You're about 808m
    // from the school - you need to be within 150m to clock in.") never reached the caller,
    // surfacing instead as a useless "Something went wrong. Please try again later." 500.
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "ACCESS_DENIED", ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", ex);
    }

    // Domain guards like Supply#collect()/MaterialSale#fulfill() throw this when the entity's
    // state no longer allows the requested transition (e.g. two concurrent "Fulfill"/"Collect"
    // requests for the same Supply race past their pre-checks and the loser hits an
    // already-exhausted quantity). That's a client-facing conflict, not a server bug - it was
    // falling through to the generic 500 handler below, showing "Something went wrong" instead of
    // the specific, actionable reason.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex);
    }

    // A request DTO's compact constructor (e.g. UpdateFeeStructureDTO, CreateFeeStructureDTO -
    // cross-field checks like "newStudentsOnly and oldStudentsOnly cannot both be true") throws
    // IllegalArgumentException while Jackson is still building the object from the request body,
    // before @Valid ever runs - Spring wraps that as HttpMessageNotReadableException rather than
    // delivering it as a bare IllegalArgumentException, so it was falling through to the generic
    // 500 handler below instead of the IllegalArgumentException one above. Unwrap it here so those
    // checks actually surface as the 400 they're meant to be.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        // Jackson wraps a compact constructor's thrown exception in its own
        // ValueInstantiationException first, which HttpMessageNotReadableException then wraps
        // again - the IllegalArgumentException we actually want is a level further down than
        // getCause() alone reaches, so walk the whole chain for it.
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof IllegalArgumentException iae) {
                return build(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", iae);
            }
            cause = cause.getCause();
        }

        log.warn("Malformed request body: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "MALFORMED_REQUEST",
                        "The request body is missing or malformed.",
                        LocalDateTime.now(),
                        null));
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
                        a -> a.getField(),
                        e -> e.getDefaultMessage(),
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
