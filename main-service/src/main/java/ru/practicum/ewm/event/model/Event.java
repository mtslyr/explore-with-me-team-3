package ru.practicum.ewm.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    User initiator;

    @Column(length = 2000)
    String annotation;

    @Column(length = 120)
    String title;

    @Column(name = "event_date")
    LocalDateTime eventDate;

    @Column(nullable = false)
    Boolean paid = false;

    @Column(nullable = false)
    Long views = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    EventState state = EventState.PENDING;

    @Column(name = "participant_limit", nullable = false)
    Integer participantLimit = 0;

    @Column(name = "request_moderation", nullable = false)
    Boolean requestModeration = true;
}
