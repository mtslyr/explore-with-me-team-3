package ru.practicum.ewm.compilations.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.common.util.EventUtil;
import ru.practicum.ewm.compilations.dto.CompilationDtoResponse;
import ru.practicum.ewm.compilations.dto.NewCompilationDto;
import ru.practicum.ewm.compilations.models.Compilation;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.rating.dto.RatingStatsDto;
import ru.practicum.ewm.rating.service.RatingService;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CompilationMapper {
    private final EventUtil eventUtil;
    private final RatingService ratingService;

    public Compilation toModel(NewCompilationDto dto) {
        return Compilation.builder()
                .pinned(dto.getPinned())
                .title(dto.getTitle())
                .build();
    }

    public CompilationDtoResponse toDto(Compilation model) {
        Map<Long, RatingStatsDto> ratings = ratingService.getStats(model.getEvents().stream()
                .map(event -> event.getEvent().getId())
                .toList());
        return CompilationDtoResponse.builder()
                .events(model.getEvents().stream()
                        .map(event
                                -> EventMapper.toEventShortDto(
                                event.getEvent(),
                                eventUtil.getViews(event.getEvent().getId()),
                                eventUtil.getConfirmedRequests(event.getEvent().getId()),
                                ratings.getOrDefault(event.getEvent().getId(), RatingStatsDto.EMPTY)))
                        .toList())
                .id(model.getId())
                .pinned(model.getPinned())
                .title(model.getTitle())
                .build();

    }
}
