package ru.practicum.ewm.compilations.services;

import ru.practicum.ewm.compilations.dto.CompilationDtoResponse;
import ru.practicum.ewm.compilations.dto.NewCompilationDto;
import ru.practicum.ewm.compilations.dto.UpdateCompilationRequest;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CompilationService {
    CompilationDtoResponse create(NewCompilationDto dto);

    void delete(long compId);

    CompilationDtoResponse patch(long compId, UpdateCompilationRequest dto);

    List<CompilationDtoResponse> get(Boolean pinned, Pageable pageable);

    CompilationDtoResponse getById(long compId);
}