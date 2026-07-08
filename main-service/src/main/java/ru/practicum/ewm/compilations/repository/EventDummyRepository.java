package ru.practicum.ewm.compilations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.compilations.models.EventDummy;

import java.util.List;

public interface EventDummyRepository extends JpaRepository<EventDummy, Long> {
    List<EventDummy> findById(List<Long> eventIds);
}
