package ru.practicum.ewm.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.LocationDto;
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
                CategoryMapper.toDto(event.getCategory()),
                confirmedRequests,
                event.getEventDate(),
                event.getId(),
                initiator,
                event.getPaid(),
                event.getTitle(),
                event.getViews()
        );
    }

    public static EventFullDto toFullDto(Event event, long confirmedRequests) {
        UserShortDto initiator = null;
        if (event.getInitiator() != null) {
            initiator = new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName());
        }

        return new EventFullDto(
                event.getAnnotation(),
                CategoryMapper.toDto(event.getCategory()),
                confirmedRequests,
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                event.getId(),
                initiator,
                new LocationDto(event.getLat(), event.getLon()),
                event.getPaid(),
                event.getParticipantLimit(),
                event.getPublishedOn(),
                event.getRequestModeration(),
                event.getState().name(),
                event.getTitle(),
                event.getViews()
        );
    }
}
