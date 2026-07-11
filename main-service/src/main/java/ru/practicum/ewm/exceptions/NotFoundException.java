package ru.practicum.ewm.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super("ERROR [404]", message, HttpStatus.NOT_FOUND);
    }
}
