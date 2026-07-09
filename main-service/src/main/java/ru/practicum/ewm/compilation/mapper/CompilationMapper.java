package ru.practicum.ewm.compilation.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;

import java.util.Comparator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompilationMapper {
    public static CompilationDto toDto(
            Compilation compilation,
            ParticipationRequestRepository requestRepository
    ) {
        List<EventShortDto> events = compilation.getEvents().stream()
                .sorted(Comparator.comparing(event -> event.getId()))
                .map(event -> EventMapper.toShortDto(
                        event,
                        requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED)
                ))
                .toList();

        return new CompilationDto(
                events,
                compilation.getId(),
                compilation.getPinned(),
                compilation.getTitle()
        );
    }
}
