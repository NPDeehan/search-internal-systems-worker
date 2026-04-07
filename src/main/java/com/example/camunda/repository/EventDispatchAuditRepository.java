package com.example.camunda.repository;

import com.example.camunda.model.EngineEventType;
import com.example.camunda.model.EventDispatchAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventDispatchAuditRepository extends JpaRepository<EventDispatchAudit, Long> {
    List<EventDispatchAudit> findByEventTypeOrderByDispatchedAtDesc(EngineEventType eventType, Pageable pageable);

    List<EventDispatchAudit> findByOrderByDispatchedAtDesc(Pageable pageable);

    long deleteByEventType(EngineEventType eventType);
}
