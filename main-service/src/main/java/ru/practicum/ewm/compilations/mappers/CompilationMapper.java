package ru.practicum.ewm.compilations.mappers;

import ru.practicum.ewm.compilations.dto.CompilationDtoResponse;
import ru.practicum.ewm.compilations.dto.NewCompilationDto;
import ru.practicum.ewm.compilations.models.Compilation;

public class CompilationMapper {
    public static Compilation toModel(NewCompilationDto dto) {
        return Compilation.builder()
                .pinned(dto.isPinned())
                .title(dto.getTitle())
                .build();
    }

    public static CompilationDtoResponse toDto(Compilation model) {
        return CompilationDtoResponse.builder()
                .events(model.getEvents().stream()
                        .map(event -> EventMapperDummy.toDto(event.getEvent()))
                        .toList())
                .id(model.getId())
                .pinned(model.isPinned())
                .title(model.getTitle())
                .build();

    }
}