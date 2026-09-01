package com.example.NexSpend.Exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ErrorResponse buildErrorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String message
    ) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String message = "Invalid request body";
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof IllegalArgumentException) {
            message = "Invalid enum value provided";
        }

        return ResponseEntity.badRequest().body(
                buildErrorResponse(request, HttpStatus.BAD_REQUEST, message)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        // Do not reveal whether an email exists or which credential failed.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                buildErrorResponse(
                        request,
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                )
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(
            DisabledException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildErrorResponse(
                        request,
                        HttpStatus.FORBIDDEN,
                        "Please verify your email before signing in"
                )
        );
    }

    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActivated(
            AccountNotActivatedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildErrorResponse(request, HttpStatus.FORBIDDEN, ex.getMessage())
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildErrorResponse(request, HttpStatus.CONFLICT, ex.getMessage())
        );
    }

    @ExceptionHandler({UserNotFoundException.class, ExpenseNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildErrorResponse(request, HttpStatus.NOT_FOUND, ex.getMessage())
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage())
        );
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenExpired(
            RefreshTokenExpiredException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                buildErrorResponse(request, HttpStatus.UNAUTHORIZED, ex.getMessage())
        );
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedActionException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildErrorResponse(request, HttpStatus.FORBIDDEN, ex.getMessage())
        );
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ErrorResponse> handleReportGeneration(
            ReportGenerationException ex,
            HttpServletRequest request
    ) {
        log.error("Report generation failed for {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildErrorResponse(
                        request,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to generate the report"
                )
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildErrorResponse(request, HttpStatus.NOT_FOUND, "Resource not found")
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(request, HttpStatus.BAD_REQUEST, "Validation error")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildErrorResponse(
                        request,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected server error occurred"
                )
        );
    }

}
