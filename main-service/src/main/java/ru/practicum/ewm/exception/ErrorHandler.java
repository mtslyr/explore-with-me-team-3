package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        log.warn("404 NOT_FOUND: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "The required object was not found.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final ConflictException e) {
        log.warn("409 CONFLICT: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(final DataIntegrityViolationException e) {
        log.warn("409 CONFLICT: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "Integrity constraint has been violated.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(final ValidationException e) {
        log.warn("400 BAD_REQUEST: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
        log.warn("400 BAD_REQUEST: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException e) {
        log.warn("400 BAD_REQUEST: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParam(final MissingServletRequestParameterException e) {
        log.warn("400 BAD_REQUEST: {}", e.getMessage());
        return new ApiError(List.of(), e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable e) {
        log.error("500 INTERNAL_SERVER_ERROR: ", e);
        return new ApiError(List.of(), e.getMessage(), "Error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
