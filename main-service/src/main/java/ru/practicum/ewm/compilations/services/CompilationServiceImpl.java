package ru.practicum.ewm.compilations.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilations.dto.CompilationDtoResponse;
import ru.practicum.ewm.compilations.dto.NewCompilationDto;
import ru.practicum.ewm.compilations.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilations.mappers.CompilationMapper;
import ru.practicum.ewm.compilations.models.Compilation;
import ru.practicum.ewm.compilations.models.CompilationEvent;
import ru.practicum.ewm.compilations.models.EventDummy;
import ru.practicum.ewm.compilations.repository.CompilationRepository;
import ru.practicum.ewm.compilations.repository.EventDummyRepository;
import ru.practicum.ewm.exceptions.CompilationNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final EventDummyRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Override
    @Transactional
    public CompilationDtoResponse create(NewCompilationDto dto) {
        log.debug("CompilationService->create: {}", dto);
        List<EventDummy> events = getEvents(dto.getEventIds());
        Compilation compilation = CompilationMapper.toModel(dto);

        if (compilation.getEvents() == null) {
            compilation.setEvents(new ArrayList<>());
        }
        createCompilationEvents(compilation, events);

        Compilation result = compilationRepository.save(compilation);
        return CompilationMapper.toDto(result);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.debug("CompilationService->delete id={}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new CompilationNotFoundException(compId));
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional
    public CompilationDtoResponse patch(Long compId, UpdateCompilationRequest dto) {
        log.debug("CompilationService->patch id={}. {}", compId, dto);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(()
                        -> new CompilationNotFoundException(compId));

        if (dto.getEventIds() != null) {
            compilation.getEvents().clear();
            List<EventDummy> events = getEvents(dto.getEventIds());
            createCompilationEvents(compilation, events);
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            compilation.setTitle(dto.getTitle());
        }

        Compilation result = compilationRepository.save(compilation);
        return CompilationMapper.toDto(result);
    }

    @Override
    public List<CompilationDtoResponse> getCompilations(Boolean pinned, Pageable pageable) {
        log.debug("CompilationService->get pinned={}, pageable={}", pinned, pageable);
        return compilationRepository.findWithOffset(pinned, pageable).stream()
                .map(CompilationMapper::toDto)
                .toList();
    }

    @Override
    public CompilationDtoResponse getById(Long compId) {
        log.debug("CompilationService->getById id={}", compId);
        Compilation result = compilationRepository.findById(compId)
                .orElseThrow(() -> new CompilationNotFoundException(compId));
        return CompilationMapper.toDto(result);
    }

    private List<EventDummy> getEvents(List<Long> eventIds) {
        return (eventIds == null || eventIds.isEmpty()) ?
                Collections.emptyList()
                : eventRepository.findByIdIn(eventIds);
    }

    private void createCompilationEvents(Compilation compilation, List<EventDummy> events) {
        events.forEach(event -> compilation.getEvents().add(
                CompilationEvent.builder()
                        .compilation(compilation)
                        .event(event)
                        .build()
        ));
    }
}