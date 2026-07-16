package ru.practicum.ewm.request.exception;

import ru.practicum.ewm.common.exception.NotFoundException;

public class RequestNotFoundException extends NotFoundException {

    public RequestNotFoundException(Long id) {
        super("Request with ID = %d not found.".formatted(id));
    }
}
