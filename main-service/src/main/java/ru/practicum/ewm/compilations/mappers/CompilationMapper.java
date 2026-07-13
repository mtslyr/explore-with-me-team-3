package ru.practicum.ewm.compilations.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.common.util.EventUtil;
import ru.practicum.ewm.compilations.dto.CompilationDtoResponse;
import ru.practicum.ewm.compilations.dto.NewCompilationDto;
import ru.practicum.ewm.compilations.models.Compilation;
import ru.practicum.ewm.event.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class CompilationMapper {
    private final EventUtil eventUtil;

    public Compilation toModel(NewCompilationDto dto) {
        return Compilation.builder()
                .pinned(dto.getPinned())
                .title(dto.getTitle())
                .build();
    }

    public CompilationDtoResponse toDto(Compilation model) {
        return CompilationDtoResponse.builder()
                .events(model.getEvents().stream()
                        .map(event
                                -> EventMapper.toEventShortDto(
                                event.getEvent(),
                                eventUtil.getViews(event.getId()),
                                eventUtil.getConfirmedRequests(event.getId())))
                        .toList())
                .id(model.getId())
                .pinned(model.getPinned())
                .title(model.getTitle())
                .build();

    }
}