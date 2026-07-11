package ru.practicum.ewm.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiException extends RuntimeException {
    String error;
    String description;
    HttpStatus status;
    public ApiException(String error, String description, HttpStatus status) {
        super(error);
        this.error = error;
        this.description = description;
        this.status = status;
    }
}
