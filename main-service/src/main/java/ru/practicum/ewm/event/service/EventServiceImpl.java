package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.error.BadRequestException;
import ru.practicum.ewm.common.error.ConflictException;
import ru.practicum.ewm.common.error.NotFoundException;
import ru.practicum.ewm.common.page.OffsetPageRequest;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventRequest;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> USER_ACTIONS = Set.of("SEND_TO_REVIEW", "CANCEL_REVIEW");
    private static final Set<String> ADMIN_ACTIONS = Set.of("PUBLISH_EVENT", "REJECT_EVENT");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        ensureUserExists(userId);
        validatePage(from, size);
        return eventRepository.findAllByInitiatorId(
                        userId,
                        new OffsetPageRequest(from, size, Sort.by("id").ascending())
                ).stream()
                .map(this::toShortDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + request.getCategory() + " was not found"));
        validateEventDate(request.getEventDate(), 2);

        Event event = new Event();
        event.setInitiator(user);
        event.setCategory(category);
        event.setAnnotation(request.getAnnotation());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setCreatedOn(LocalDateTime.now());
        event.setLat(request.getLocation().getLat());
        event.setLon(request.getLocation().getLon());
        event.setPaid(Boolean.TRUE.equals(request.getPaid()));
        event.setParticipantLimit(request.getParticipantLimit() == null ? 0 : request.getParticipantLimit());
        event.setRequestModeration(request.getRequestModeration() == null || request.getRequestModeration());
        event.setTitle(request.getTitle());
        event.setState(EventState.PENDING);
        return toFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        ensureUserExists(userId);
        return toFullDto(eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> eventNotFound(eventId)));
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventRequest request) {
        ensureUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> eventNotFound(eventId));
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        applyCommonUpdate(event, request, 2);
        if (request.getStateAction() != null) {
            if (!USER_ACTIONS.contains(request.getStateAction())) {
                throw new BadRequestException("Unknown state action: " + request.getStateAction());
            }
            if ("SEND_TO_REVIEW".equals(request.getStateAction())) {
                event.setState(EventState.PENDING);
            } else {
                event.setState(EventState.CANCELED);
            }
        }
        return toFullDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            String rangeStart,
            String rangeEnd,
            int from,
            int size
    ) {
        validatePage(from, size);
        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        return slice(filterEvents(users, states, categories, null, start, end, null), from, size)
                .map(this::toFullDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> eventNotFound(eventId));
        applyCommonUpdate(event, request, 1);
        if (request.getStateAction() != null) {
            if (!ADMIN_ACTIONS.contains(request.getStateAction())) {
                throw new BadRequestException("Unknown state action: " + request.getStateAction());
            }
            if ("PUBLISH_EVENT".equals(request.getStateAction())) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the published event");
                }
                event.setState(EventState.CANCELED);
            }
        }
        return toFullDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    ) {
        validatePage(from, size);
        LocalDateTime start = rangeStart == null ? LocalDateTime.now() : parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        validateRange(start, end);
        Stream<Event> events = filterEvents(
                null,
                List.of(EventState.PUBLISHED.name()),
                categories,
                paid,
                start,
                end,
                text
        );
        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.filter(event -> event.getParticipantLimit() == 0
                    || confirmed(event) < event.getParticipantLimit());
        }
        if ("VIEWS".equals(sort)) {
            events = events.sorted(Comparator.comparing(Event::getViews));
        } else {
            events = events.sorted(Comparator.comparing(Event::getEventDate));
        }
        return slice(events, from, size)
                .map(this::toShortDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(found -> found.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> eventNotFound(eventId));
        if (event.getViews() == 0) {
            event.setViews(1L);
        }
        return toFullDto(event);
    }

    private void applyCommonUpdate(Event event, UpdateEventRequest request, int hours) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Category with id=" + request.getCategory() + " was not found"
                    )));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate(), hours);
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLat(request.getLocation().getLat());
            event.setLon(request.getLocation().getLon());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    private Stream<Event> filterEvents(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            Boolean paid,
            LocalDateTime start,
            LocalDateTime end,
            String text
    ) {
        Stream<Event> events = eventRepository.findAll().stream();
        if (users != null && !users.isEmpty()) {
            events = events.filter(event -> users.contains(event.getInitiator().getId()));
        }
        if (states != null && !states.isEmpty()) {
            events = events.filter(event -> states.contains(event.getState().name()));
        }
        if (categories != null && !categories.isEmpty()) {
            events = events.filter(event -> categories.contains(event.getCategory().getId()));
        }
        if (paid != null) {
            events = events.filter(event -> paid.equals(event.getPaid()));
        }
        if (start != null) {
            events = events.filter(event -> !event.getEventDate().isBefore(start));
        }
        if (end != null) {
            events = events.filter(event -> !event.getEventDate().isAfter(end));
        }
        if (text != null && !text.isBlank()) {
            String lowerText = text.toLowerCase(Locale.ROOT);
            events = events.filter(event -> event.getAnnotation().toLowerCase(Locale.ROOT).contains(lowerText)
                    || event.getDescription().toLowerCase(Locale.ROOT).contains(lowerText));
        }
        return events.sorted(Comparator.comparing(Event::getId));
    }

    private Stream<Event> slice(Stream<Event> events, int from, int size) {
        return events.skip(from).limit(size);
    }

    private EventShortDto toShortDto(Event event) {
        return EventMapper.toShortDto(event, confirmed(event));
    }

    private EventFullDto toFullDto(Event event) {
        return EventMapper.toFullDto(event, confirmed(event));
    }

    private long confirmed(Event event) {
        return requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
    }

    private LocalDateTime parseDate(String value) {
        return value == null ? null : LocalDateTime.parse(value, FORMATTER);
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new BadRequestException("Event date must be at least " + hours + " hours after now");
        }
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Range start must be before range end");
        }
    }

    private void validatePage(int from, int size) {
        if (from < 0 || size < 1) {
            throw new BadRequestException("Invalid pagination parameters");
        }
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private NotFoundException eventNotFound(Long eventId) {
        return new NotFoundException("Event with id=" + eventId + " was not found");
    }
}
