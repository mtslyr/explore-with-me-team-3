package ru.practicum.ewm.exceptions;

public class CompilationNotFoundException extends NotFoundException {
    public CompilationNotFoundException(Long id) {
        super("Compilation with id %d not found".formatted(id));
    }
}
