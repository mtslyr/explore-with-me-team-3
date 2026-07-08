package ru.practicum.ewm.compilations.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@ToString
public class CompilationDtoResponse {
    private long id;
    private boolean pinned;
    private String title;
    private List<EventDtoDummy> events;
}