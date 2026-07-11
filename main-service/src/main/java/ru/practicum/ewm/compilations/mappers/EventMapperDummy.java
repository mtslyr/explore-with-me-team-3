package ru.practicum.ewm.compilations.mappers;

import ru.practicum.ewm.compilations.dto.EventDtoDummy;
import ru.practicum.ewm.compilations.models.EventDummy;

public class EventMapperDummy {
    public static EventDtoDummy toDto(EventDummy model) {
        return EventDtoDummy.builder()
                .id(model.getId())
                .build();
    }
}