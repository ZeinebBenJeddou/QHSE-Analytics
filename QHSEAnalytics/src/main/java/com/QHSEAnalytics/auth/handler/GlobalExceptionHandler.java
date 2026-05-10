package com.QHSEAnalytics.auth.handler;

import com.QHSEAnalytics.auth.exception.*;
import com.QHSEAnalytics.shared.exception.CategorieNotFoundException;
import com.QHSEAnalytics.shared.exception.AdminProtectedException;
import com.QHSEAnalytics.shared.exception.AnalyseNotFoundException;
import com.QHSEAnalytics.shared.exception.FileTooLargeException;
import com.QHSEAnalytics.shared.exception.ImportNotFoundException;
import com.QHSEAnalytics.shared.exception.ImportNotReadyException;
import com.QHSEAnalytics.shared.exception.ImportTransitionException;
import com.QHSEAnalytics.shared.exception.ImportValidationException;
import com.QHSEAnalytics.shared.exception.InvalidFileFormatException;
import com.QHSEAnalytics.shared.exception.InvalidSeuilException;
import com.QHSEAnalytics.shared.exception.KpiAlreadyExistsException;
import com.QHSEAnalytics.shared.exception.KpiNotFoundException;
import com.QHSEAnalytics.shared.exception.PdfGenerationException;
import com.QHSEAnalytics.shared.exception.UserAlreadyActiveException;
import com.QHSEAnalytics.shared.exception.UserAlreadyAdminException;
import com.QHSEAnalytics.shared.exception.UserAlreadyAnalysteException;
import com.QHSEAnalytics.shared.exception.UserAlreadyInactiveException;
import com.QHSEAnalytics.shared.exception.WrongPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(KpiNotFoundException.class)
    public ResponseEntity<?> handleKpiNotFound(KpiNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CategorieNotFoundException.class)
    public ResponseEntity<?> handleCategorieNotFound(CategorieNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(KpiAlreadyExistsException.class)
    public ResponseEntity<?> handleKpiAlreadyExists(KpiAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidSeuilException.class)
    public ResponseEntity<?> handleInvalidSeuil(InvalidSeuilException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<?> handleAccountNotVerified(AccountNotVerifiedException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<?> handleTokenExpired(TokenExpiredException ex) {
        return buildError(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex) {
        return buildError(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler({UserNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<?> handleUnauthorized(RuntimeException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
    }

    @ExceptionHandler(RefreshTokenInvalidException.class)
    public ResponseEntity<?> handleInvalidRefreshToken(RefreshTokenInvalidException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({PasswordMismatchException.class, InvalidTokenException.class})
    public ResponseEntity<?> handleBusinessBadRequest(RuntimeException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({InvalidFileFormatException.class, ImportValidationException.class})
    public ResponseEntity<?> handleImportBadRequest(RuntimeException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<?> handleFileTooLarge(FileTooLargeException ex) {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(ImportNotFoundException.class)
    public ResponseEntity<?> handleImportNotFound(RuntimeException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ImportNotReadyException.class)
    public ResponseEntity<?> handleImportNotReady(ImportNotReadyException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ImportTransitionException.class)
    public ResponseEntity<?> handleImportTransition(ImportTransitionException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AnalyseNotFoundException.class)
    public ResponseEntity<?> handleAnalyseNotFound(AnalyseNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AdminProtectedException.class)
    public ResponseEntity<?> handleAdminProtected(AdminProtectedException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ResponseEntity<?> handleUserAlreadyActive(UserAlreadyActiveException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<?> handleUserAlreadyInactive(UserAlreadyInactiveException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyAdminException.class)
    public ResponseEntity<?> handleUserAlreadyAdmin(UserAlreadyAdminException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyVerifiedException.class)
    public ResponseEntity<?> handleUserAlreadyVerified(UserAlreadyVerifiedException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyAnalysteException.class)
    public ResponseEntity<?> handleUserAlreadyAnalyste(UserAlreadyAnalysteException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<?> handleWrongPassword(WrongPasswordException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<?> handlePdfGeneration(PdfGenerationException ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> {
            String message = err.getDefaultMessage() == null ? "Valeur invalide" : err.getDefaultMessage();
            fieldErrors.put(err.getField(), message);
        });

        ErrorResponse payload = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Requête invalide")
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Requête invalide." : ex.getReason();
        return buildError(HttpStatus.valueOf(ex.getStatusCode().value()), message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        ErrorResponse payload = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(payload);
    }
}
