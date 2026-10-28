package com.qeema.util.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.qeema.util.common.api.ApiError;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException exception,
                        WebRequest request) {
                Map<String, String> fieldErrors = new LinkedHashMap<>();
                exception.getBindingResult().getFieldErrors()
                                .forEach(error -> fieldErrors.putIfAbsent(
                                                error.getField(),
                                                error.getDefaultMessage()));

                return response(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed",
                                request,
                                fieldErrors);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiError> handleConstraintViolation(
                        ConstraintViolationException exception,
                        WebRequest request) {
                Map<String, String> fieldErrors = new LinkedHashMap<>();
                exception.getConstraintViolations()
                                .forEach(violation -> fieldErrors.putIfAbsent(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()));

                return response(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed",
                                request,
                                fieldErrors);
        }

        @ExceptionHandler({
                        HttpMessageNotReadableException.class,
                        MethodArgumentTypeMismatchException.class
        })
        public ResponseEntity<ApiError> handleMalformedRequest(
                        Exception exception,
                        WebRequest request) {
                return response(
                                HttpStatus.BAD_REQUEST,
                                "Malformed request",
                                request,
                                Map.of());
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleNotFound(
                        ResourceNotFoundException exception,
                        WebRequest request) {
                return response(
                                HttpStatus.NOT_FOUND,
                                exception.getMessage(),
                                request,
                                Map.of());
        }

        @ExceptionHandler(InvalidIdentityTokenException.class)
        public ResponseEntity<ApiError> handleInvalidIdentityToken(
                        InvalidIdentityTokenException exception,
                        WebRequest request) {
                return response(
                                HttpStatus.UNAUTHORIZED,
                                "The access token is missing required identity claims",
                                request,
                                Map.of());
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(
                AccessDeniedException exception,
                WebRequest request) {
            return response(
                                HttpStatus.FORBIDDEN,
                                "You are not authorized to access this resource",
                                request,
                                Map.of());
        }

        @ExceptionHandler(DocumentStorageException.class)
        public ResponseEntity<ApiError> handleDocumentStorage(
                DocumentStorageException exception,
                WebRequest request) {
            logger.error("Document storage operation failed", exception);
            return response(
                                HttpStatus.BAD_GATEWAY,
                                "Document storage is currently unavailable",
                                request,
                                Map.of());
        }

        @ExceptionHandler(EmailSendingException.class)
        public ResponseEntity<ApiError> handleEmailSending(
                EmailSendingException exception,
                WebRequest request) {
            return response(
                                HttpStatus.BAD_GATEWAY,
                                "The email provider could not accept the message",
                                request,
                                Map.of());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgument(
                        IllegalArgumentException exception,
                        WebRequest request) {
                return response(
                                HttpStatus.BAD_REQUEST,
                                exception.getMessage(),
                                request,
                                Map.of());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleUnexpected(
                        Exception exception,
                        WebRequest request) {
                logger.error("Unhandled exception while processing request", exception);
                return response(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred",
                                request,
                                Map.of());
        }

        private ResponseEntity<ApiError> response(
                        HttpStatus status,
                        String message,
                        WebRequest request,
                        Map<String, String> fieldErrors) {
                String path = request instanceof ServletWebRequest servletRequest
                                ? servletRequest.getRequest().getRequestURI()
                                : "";

                ApiError error = new ApiError(
                                java.time.Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                path,
                                fieldErrors);

                return ResponseEntity.status(status).body(error);
        }
}
