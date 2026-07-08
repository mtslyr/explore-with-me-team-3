package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.error.BadRequestException;
import ru.practicum.ewm.common.error.ConflictException;
import ru.practicum.ewm.common.error.NotFoundException;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        findUser(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        User requester = findUser(userId);
        Event event = findEvent(eventId);
        validateRequestCreation(userId, event);

        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setRequester(requester);
        request.setEvent(event);
        request.setStatus(isConfirmationRequired(event) ? RequestStatus.PENDING : RequestStatus.CONFIRMED);

        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        findUser(userId);
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        request.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        findUser(userId);
        findOwnedEvent(userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        findUser(userId);
        Event event = findOwnedEvent(userId, eventId);
        if (updateRequest.getStatus() == null) {
            throw new BadRequestException("Request status is required");
        }
        List<ParticipationRequest> requests = getRequestsForUpdate(eventId, updateRequest);
        ensureAllPending(requests);

        if (updateRequest.getStatus() == RequestStatus.REJECTED) {
            requests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            return result(List.of(), requests);
        }

        if (updateRequest.getStatus() != RequestStatus.CONFIRMED) {
            throw new BadRequestException("Unsupported request status: " + updateRequest.getStatus());
        }

        return confirmRequests(event, requests);
    }

    private EventRequestStatusUpdateResult confirmRequests(Event event, List<ParticipationRequest> requests) {
        int participantLimit = event.getParticipantLimit();
        if (participantLimit == 0) {
            requests.forEach(request -> request.setStatus(RequestStatus.CONFIRMED));
            return result(requests, List.of());
        }

        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (confirmed >= participantLimit) {
            throw new ConflictException("The participant limit has been reached");
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();
        long freePlaces = participantLimit - confirmed;

        for (ParticipationRequest request : requests) {
            if (freePlaces > 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(request);
                freePlaces--;
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        if (freePlaces == 0) {
            List<ParticipationRequest> pending = requestRepository.findAllByEventIdAndStatus(
                    event.getId(),
                    RequestStatus.PENDING
            );
            pending.forEach(request -> request.setStatus(RequestStatus.REJECTED));
        }

        return result(confirmedRequests, rejectedRequests);
    }

    private void validateRequestCreation(Long userId, Event event) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Event initiator cannot add request to own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event must be published");
        }
        if (requestRepository.existsByEventIdAndRequesterId(event.getId(), userId)) {
            throw new ConflictException("Request already exists");
        }
        if (event.getParticipantLimit() > 0
                && requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED)
                >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }
    }

    private boolean isConfirmationRequired(Event event) {
        return event.getParticipantLimit() > 0 && Boolean.TRUE.equals(event.getRequestModeration());
    }

    private List<ParticipationRequest> getRequestsForUpdate(Long eventId,
                                                            EventRequestStatusUpdateRequest updateRequest) {
        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            return List.of();
        }
        return requestRepository.findAllByEventIdAndIdIn(eventId, updateRequest.getRequestIds());
    }

    private void ensureAllPending(List<ParticipationRequest> requests) {
        boolean hasNotPending = requests.stream()
                .anyMatch(request -> request.getStatus() != RequestStatus.PENDING);
        if (hasNotPending) {
            throw new BadRequestException("Request must have status PENDING");
        }
    }

    private EventRequestStatusUpdateResult result(List<ParticipationRequest> confirmed,
                                                  List<ParticipationRequest> rejected) {
        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(ParticipationRequestMapper::toDto).toList(),
                rejected.stream().map(ParticipationRequestMapper::toDto).toList()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Event findOwnedEvent(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }
}
