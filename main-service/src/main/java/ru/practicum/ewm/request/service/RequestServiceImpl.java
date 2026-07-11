package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.event.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot add request to own event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }

        if (event.getParticipantLimit() != 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        RequestStatus status = event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED;

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        request = requestRepository.save(request);
        log.debug("Request created: {} for event {}", request.getId(), eventId);
        return RequestMapper.toRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getAllByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        return requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " was not found");
        }

        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        log.debug("Request canceled: {}", requestId);
        return RequestMapper.toRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getByEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        return requestRepository.findByEventId(eventId).stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeStatus(Long userId, Long eventId,
                                                         EventRequestStatusUpdateRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Confirmation of requests is not required");
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        long limit = event.getParticipantLimit();

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (Long requestId : dto.getRequestIds()) {
            ParticipationRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }

            if ("CONFIRMED".equals(dto.getStatus())) {
                if (confirmed >= limit) {
                    request.setStatus(RequestStatus.REJECTED);
                    requestRepository.save(request);
                    rejectedRequests.add(RequestMapper.toRequestDto(request));
                    continue;
                }
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed++;
            } else {
                request.setStatus(RequestStatus.REJECTED);
            }

            request = requestRepository.save(request);

            if (request.getStatus() == RequestStatus.CONFIRMED) {
                confirmedRequests.add(RequestMapper.toRequestDto(request));
            } else {
                rejectedRequests.add(RequestMapper.toRequestDto(request));
            }
        }

        if (confirmed >= limit) {
            List<ParticipationRequest> pending = requestRepository.findByEventId(eventId).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .collect(Collectors.toList());
            for (ParticipationRequest r : pending) {
                r.setStatus(RequestStatus.REJECTED);
                requestRepository.save(r);
                rejectedRequests.add(RequestMapper.toRequestDto(r));
            }
        }

        log.debug("Requests confirmed: {}, rejected: {}", confirmedRequests.size(), rejectedRequests.size());
        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }
}
