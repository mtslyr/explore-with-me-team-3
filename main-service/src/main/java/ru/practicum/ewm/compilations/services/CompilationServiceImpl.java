package ru.practicum.ewm.compilations.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import ru.practicum.ewm.exceptions.NotFoundException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Qualifier("CompilationServiceImpl")
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final EventDummyRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Override
    @Transactional
    public CompilationDtoResponse create(NewCompilationDto dto) {
        log.info("CompilationService->create: {}", dto);
        List<EventDummy> events = getEvents(dto.getEventIds());
        log.info("getEvents {}", events.size());
        Compilation compilation = CompilationMapper.toModel(dto);
        createCompilationEvent(compilation, events);

        Compilation result = compilationRepository.save(compilation);

        log.info("compilationRepository.save {}", result);
        return CompilationMapper.toDto(result);
    }

    @Override
    @Transactional
    public void delete(long compId) {
        log.info("CompilationService->delete id={}", compId);
        checkCompilationExist(compId);

        compilationRepository.deleteById(compId);

        log.info("compilationRepository.deleteById id={}", compId);
    }

    @Override
    @Transactional
    public CompilationDtoResponse patch(long compId, UpdateCompilationRequest dto) {
        log.info("CompilationService->patch id={}. {}", compId, dto);
        Compilation compilation = getCompilation(compId);
        log.info("getCompilation: {}", compilation);
        Compilation update = updateCompilation(compilation, dto);
        log.info("updateCompilation: {}", update);

        Compilation result = compilationRepository.save(update);

        log.info("compilationRepository.save {}", result);
        return CompilationMapper.toDto(result);
    }

    @Override
    public List<CompilationDtoResponse> get(Boolean pinned, Pageable pageable) {
        log.info("CompilationService->get pinned={}, pageable={}", pinned, pageable);
        List<Compilation> result = compilationRepository.findWithOffset(pinned, pageable);
        log.info("compilationRepository.findWithOffset {}", result.size());
        return result.stream()
                .map(CompilationMapper::toDto)
                .toList();
    }

    @Override
    public CompilationDtoResponse getById(long compId) {
        log.info("CompilationService->getById id={}", compId);
        Compilation result = getCompilation(compId);
        log.info("getCompilation {}", result);
        return CompilationMapper.toDto(result);
    }

    private List<EventDummy> getEvents(List<Long> eventIds) {
        return eventIds == null ? List.of() : eventRepository.findByIdIn(eventIds);
    }

    private Compilation getCompilation(long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(logInfo("compilation with id=" + id + " doesn't exist")));
    }

    private Compilation updateCompilation(Compilation compilation, UpdateCompilationRequest update) {
        Optional.ofNullable(update.getEventIds())
                .map(this::getEvents)
                .ifPresent(events -> createCompilationEvent(compilation, events));

        Optional.of(update.isPinned())
                .ifPresent(compilation::setPinned);

        Optional.ofNullable(update.getTitle())
                .ifPresent(compilation::setTitle);

        return compilation;
    }

    private void createCompilationEvent(Compilation compilation, List<EventDummy> events) {
        events.forEach(event -> {
                    compilation.getEvents().add(
                            CompilationEvent.builder()
                                    .compilation(compilation)
                                    .event(event)
                                    .build());
                }
        );
    }

    private void checkCompilationExist(long id) {
        if (!compilationRepository.existsById(id)) {
            throw new NotFoundException(logInfo("compilation with id=" + id + " doesn't exist"));
        }
    }

    private String logInfo(String message) {
        log.info(message);
        return message;
    }

}
