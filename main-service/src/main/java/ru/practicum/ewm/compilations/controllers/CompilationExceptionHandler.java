package ru.practicum.ewm.compilations.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.exceptions.ApiException;
import ru.practicum.ewm.exceptions.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class CompilationExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(final ApiException e) {
        log.info("API Exception [{}]: {}", e.getStatus(), e.getMessage(), e);
        ErrorResponse response = new ErrorResponse(e.getError(), e.getDescription());
        return ResponseEntity
                .status(e.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(final Exception e) {
        log.info("Unhandled Exception: {}", e.getMessage(), e);
        return new ErrorResponse("INTERNAL_SERVER_ERROR", e.getMessage());
    }
}