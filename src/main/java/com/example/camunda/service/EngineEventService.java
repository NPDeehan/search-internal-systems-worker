package com.example.camunda.service;

import com.example.camunda.model.EngineEventType;
import com.example.camunda.model.EventDispatchAudit;
import com.example.camunda.model.EventPreset;
import com.example.camunda.repository.EventDispatchAuditRepository;
import com.example.camunda.repository.EventPresetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EngineEventService {

    private static final Logger logger = LoggerFactory.getLogger(EngineEventService.class);

    private final ZeebeConnectionService zeebeConnectionService;
    private final EventPresetRepository eventPresetRepository;
    private final EventDispatchAuditRepository eventDispatchAuditRepository;
    private final ObjectMapper objectMapper;

    public EngineEventService(ZeebeConnectionService zeebeConnectionService,
                              EventPresetRepository eventPresetRepository,
                              EventDispatchAuditRepository eventDispatchAuditRepository,
                              ObjectMapper objectMapper) {
        this.zeebeConnectionService = zeebeConnectionService;
        this.eventPresetRepository = eventPresetRepository;
        this.eventDispatchAuditRepository = eventDispatchAuditRepository;
        this.objectMapper = objectMapper;
    }

    public List<EventPreset> listPresets(EngineEventType eventType) {
        return eventPresetRepository.findByEventTypeOrderByNameAsc(eventType);
    }

    public EventPreset savePreset(EventPreset preset) {
        Optional<EventPreset> existing = eventPresetRepository.findByEventTypeAndNameIgnoreCase(
                preset.getEventType(),
                preset.getName()
        );

        if (existing.isPresent()) {
            EventPreset toUpdate = existing.get();
            toUpdate.setEventName(preset.getEventName());
            toUpdate.setCorrelationKey(preset.getCorrelationKey());
            toUpdate.setMessageId(preset.getMessageId());
            toUpdate.setTimeToLiveMs(preset.getTimeToLiveMs());
            toUpdate.setPayloadJson(preset.getPayloadJson());
            return eventPresetRepository.save(toUpdate);
        }

        return eventPresetRepository.save(preset);
    }

    public void deletePreset(Long id) {
        eventPresetRepository.deleteById(id);
    }

    public Map<String, Object> publishEvent(EngineEventType eventType,
                                            String eventName,
                                            String correlationKey,
                                            String messageId,
                                            Long timeToLiveMs,
                                            Map<String, Object> payload) {
        String payloadJson = writeAsJson(payload);

        try {
            Map<String, Object> zeebeResponse;
            if (eventType == EngineEventType.MESSAGE) {
                zeebeResponse = zeebeConnectionService.publishMessage(
                        eventName,
                        correlationKey,
                        payload,
                        messageId,
                        timeToLiveMs
                );
            } else {
                zeebeResponse = zeebeConnectionService.broadcastSignal(eventName, payload);
            }

            saveDispatchAudit(eventType, eventName, correlationKey, payloadJson, zeebeResponse, true, null);
            return zeebeResponse;
        } catch (Exception ex) {
            logger.error("Failed to publish {} event '{}'", eventType, eventName, ex);
            saveDispatchAudit(eventType, eventName, correlationKey, payloadJson, null, false, ex.getMessage());
            throw ex;
        }
    }

    public List<EventDispatchAudit> listDispatches(EngineEventType eventType, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        if (eventType == null) {
            return eventDispatchAuditRepository.findByOrderByDispatchedAtDesc(pageRequest);
        }

        return eventDispatchAuditRepository.findByEventTypeOrderByDispatchedAtDesc(eventType, pageRequest);
    }

    public long clearDispatches(EngineEventType eventType) {
        if (eventType == null) {
            long count = eventDispatchAuditRepository.count();
            eventDispatchAuditRepository.deleteAllInBatch();
            return count;
        }

        return eventDispatchAuditRepository.deleteByEventType(eventType);
    }

    private void saveDispatchAudit(EngineEventType eventType,
                                   String eventName,
                                   String correlationKey,
                                   String payloadJson,
                                   Map<String, Object> response,
                                   boolean success,
                                   String errorMessage) {
        EventDispatchAudit audit = new EventDispatchAudit();
        audit.setEventType(eventType);
        audit.setEventName(eventName);
        audit.setCorrelationKey(correlationKey);
        audit.setRequestPayloadJson(payloadJson);
        audit.setResponseJson(response == null ? null : writeAsJson(response));
        audit.setSuccess(success);
        audit.setErrorMessage(errorMessage);
        audit.setDispatchedAt(LocalDateTime.now());
        eventDispatchAuditRepository.save(audit);
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            logger.warn("Falling back to compact JSON serialization", ex);
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException innerEx) {
                logger.error("Unable to serialize JSON payload", innerEx);
                return "{}";
            }
        }
    }
}
