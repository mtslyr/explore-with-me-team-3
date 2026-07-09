package ru.practicum.ewm.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.dto.UserShortDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventMapper {
    public static EventShortDto toShortDto(Event event, long confirmedRequests) {
        UserShortDto initiator = null;
        if (event.getInitiator() != null) {
            initiator = new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName());
        }

        return new EventShortDto(
                event.getAnnotation(),
                null,
                confirmedRequests,
                event.getEventDate(),
                event.getId(),
                initiator,
                event.getPaid(),
                event.getTitle(),
                event.getViews()
        );
    }
}
