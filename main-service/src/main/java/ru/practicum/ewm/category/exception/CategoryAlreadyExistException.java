package ru.practicum.ewm.category.exception;

import ru.practicum.ewm.common.exception.ConflictException;

public class CategoryAlreadyExistException extends ConflictException {
    public CategoryAlreadyExistException(String categoryName) {
        super("Категория \"%s\" существует".formatted(categoryName));
    }
}
