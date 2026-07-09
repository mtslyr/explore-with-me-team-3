package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventRequest;

import java.util.List;

public interface EventService {
    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto create(Long userId, NewEventDto request);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventRequest request);

    List<EventFullDto> getAdminEvents(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            String rangeStart,
            String rangeEnd,
            int from,
            int size
    );

    EventFullDto updateAdminEvent(Long eventId, UpdateEventRequest request);

    List<EventShortDto> getPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    );

    EventFullDto getPublicEvent(Long eventId);
}
