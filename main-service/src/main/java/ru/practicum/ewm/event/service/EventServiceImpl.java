package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.stats.clients.StatsClient;
import ru.practicum.ewm.stats.dto.StatHitResponseElement;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long HOURS_BEFORE_EVENT = 2;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), FORMATTER);
        if (eventDate.isBefore(LocalDateTime.now().plusHours(HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + dto.getEventDate());
        }

        Event event = EventMapper.toEvent(dto, category, user);
        event = eventRepository.save(event);
        log.debug("Event created: {} by user {}", event.getId(), userId);
        return EventMapper.toEventFullDto(event, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, page).getContent();
        return events.stream()
                .map(e -> EventMapper.toEventShortDto(e, getViews(e.getId()), getConfirmedRequests(e.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getByUserAndEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return EventMapper.toEventFullDto(event, getViews(eventId), getConfirmedRequests(eventId));
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (dto.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(dto.getEventDate(), FORMATTER);
            if (newDate.isBefore(LocalDateTime.now().plusHours(HOURS_BEFORE_EVENT))) {
                throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + dto.getEventDate());
            }
        }

        Category category = null;
        if (dto.getCategory() != null) {
            category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));
        }

        EventMapper.updateEventFromUserRequest(event, dto, category);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Unknown state action: " + dto.getStateAction());
            }
        }

        event = eventRepository.save(event);
        log.debug("Event updated by user: {}", eventId);
        return EventMapper.toEventFullDto(event, getViews(eventId), getConfirmedRequests(eventId));
    }

    @Override
    public List<EventFullDto> getByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          String rangeStart, String rangeEnd, int from, int size) {
        List<EventState> stateEnums = null;
        if (states != null && !states.isEmpty()) {
            stateEnums = states.stream().map(EventState::valueOf).collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findEventsByAdmin(users, stateEnums, categories, start, end, page);
        return events.stream()
                .map(e -> EventMapper.toEventFullDto(e, getViews(e.getId()), getConfirmedRequests(e.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Category category = null;
        if (dto.getCategory() != null) {
            category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));
        }

        if (dto.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(dto.getEventDate(), FORMATTER);
            if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + dto.getEventDate());
            }
        }

        EventMapper.updateEventFromAdminRequest(event, dto, category);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Event date must be at least 1 hour from now");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Unknown state action: " + dto.getStateAction());
            }
        }

        event = eventRepository.save(event);
        log.debug("Event updated by admin: {}", eventId);
        return EventMapper.toEventFullDto(event, getViews(eventId), getConfirmedRequests(eventId));
    }

    @Override
    public List<EventShortDto> getPublished(String text, List<Long> categories, Boolean paid,
                                             String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                             String sort, int from, int size) {
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        if (start == null && end == null) {
            start = LocalDateTime.now();
        }

        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findPublishedEvents(
                EventState.PUBLISHED, text, categories, paid, start, end, page);

        List<EventShortDto> result = new ArrayList<>();
        for (Event e : events) {
            long confirmed = getConfirmedRequests(e.getId());
            if (Boolean.TRUE.equals(onlyAvailable) && e.getParticipantLimit() != 0
                    && confirmed >= e.getParticipantLimit()) {
                continue;
            }
            result.add(EventMapper.toEventShortDto(e, getViews(e.getId()), confirmed));
        }

        if ("VIEWS".equals(sort)) {
            result.sort(Comparator.comparingLong(EventShortDto::getViews));
        }

        return result;
    }

    @Override
    public EventFullDto getPublishedById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        return EventMapper.toEventFullDto(event, getViews(id), getConfirmedRequests(id));
    }

    private long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private long getViews(Long eventId) {
        try {
            List<StatHitResponseElement> stats = statsClient.getStats(
                    LocalDateTime.of(2020, 1, 1, 0, 0),
                    LocalDateTime.now(),
                    List.of("/events/" + eventId),
                    true
            );
            return stats.isEmpty() ? 0 : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Failed to get views for event {}: {}", eventId, e.getMessage());
            return 0;
        }
    }
}
