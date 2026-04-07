package com.example.camunda.repository;

import com.example.camunda.model.EngineEventType;
import com.example.camunda.model.EventPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventPresetRepository extends JpaRepository<EventPreset, Long> {
    List<EventPreset> findByEventTypeOrderByNameAsc(EngineEventType eventType);

    Optional<EventPreset> findByEventTypeAndNameIgnoreCase(EngineEventType eventType, String name);
}
