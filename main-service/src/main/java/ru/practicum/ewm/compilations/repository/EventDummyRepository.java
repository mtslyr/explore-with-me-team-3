package ru.practicum.ewm.compilations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.compilations.models.EventDummy;

import java.util.List;

@Repository
public interface EventDummyRepository extends JpaRepository<EventDummy, Long> {
    List<EventDummy> findByIdIn(List<Long> eventIds);
}
