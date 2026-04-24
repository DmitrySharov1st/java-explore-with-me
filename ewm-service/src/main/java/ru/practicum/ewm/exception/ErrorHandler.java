package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.dto.ApiError;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ApiError buildApiError(HttpStatus status, String reason, String message, List<String> errors) {
        return ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .errors(errors)
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .collect(Collectors.toList());
        log.error("Validation error: {}", errors);
        return buildApiError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage(), errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Bad request: {}", e.getMessage());
        return buildApiError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage(), List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.error("Not found: {}", e.getMessage());
        return buildApiError(HttpStatus.NOT_FOUND, "The required object was not found.", e.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.error("Conflict: {}", e.getMessage());
        return buildApiError(HttpStatus.CONFLICT, "Integrity constraint has been violated.",
                e.getMessage(), List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleForbidden(ForbiddenException e) {
        log.error("Forbidden: {}", e.getMessage());
        return buildApiError(HttpStatus.CONFLICT, "For the requested operation the conditions are not met.",
                e.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(Exception e) {
        log.error("Internal error", e);
        return buildApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e.getMessage(), List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParams(MissingServletRequestParameterException e) {
        log.error("Missing request parameter: {}", e.getMessage());
        return buildApiError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("Type mismatch: {}", e.getMessage());
        return buildApiError(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNotReadable(HttpMessageNotReadableException e) {
        log.error("Malformed JSON request: {}", e.getMessage());
        return buildApiError(HttpStatus.BAD_REQUEST, "Malformed JSON request.", e.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage());
        return buildApiError(HttpStatus.CONFLICT, "Integrity constraint has been violated.",
                e.getMessage(), List.of());
    }
}