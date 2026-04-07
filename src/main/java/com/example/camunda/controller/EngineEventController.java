package com.example.camunda.controller;

import com.example.camunda.model.EngineEventType;
import com.example.camunda.model.EventDispatchAudit;
import com.example.camunda.model.EventPreset;
import com.example.camunda.service.EngineEventService;
import com.example.camunda.service.ZeebeConnectionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/engine")
public class EngineEventController {

    private final ZeebeConnectionService zeebeConnectionService;
    private final EngineEventService engineEventService;

    public EngineEventController(ZeebeConnectionService zeebeConnectionService, EngineEventService engineEventService) {
        this.zeebeConnectionService = zeebeConnectionService;
        this.engineEventService = engineEventService;
    }

    @PostMapping("/events/publish")
    public Map<String, Object> publishEngineEvent(@RequestBody Map<String, Object> request) {
        EngineEventType eventType = extractEventType(request.get("eventType"));
        String eventName = extractRequiredString(request.get("name"), "name");
        Map<String, Object> variables = extractVariablesMap(request.get("variables"));

        if (!zeebeConnectionService.isConnected()) {
            throw new IllegalStateException("Zeebe connection is not available");
        }

        String correlationKey = null;
        String messageId = null;
        Long timeToLiveMs = null;

        if (eventType == EngineEventType.MESSAGE) {
            correlationKey = extractRequiredString(request.get("correlationKey"), "correlationKey");
            messageId = extractOptionalString(request.get("messageId"));
            timeToLiveMs = extractOptionalLong(request.get("timeToLiveMs"));
        }

        Map<String, Object> details = engineEventService.publishEvent(
                eventType,
                eventName,
                correlationKey,
                messageId,
                timeToLiveMs,
                variables
        );

        Map<String, Object> result = new HashMap<>();
        result.put("accepted", true);
        result.put("eventType", eventType.name());
        result.put("name", eventName);
        result.put("timestamp", OffsetDateTime.now().toString());
        result.put("details", details);

        if (eventType == EngineEventType.MESSAGE) {
            result.put("correlationKey", correlationKey);
            result.put("messageId", messageId);
            result.put("timeToLiveMs", timeToLiveMs);
        }

        return result;
    }

    @GetMapping("/event-presets")
    public List<Map<String, Object>> listEventPresets(@RequestParam String eventType) {
        EngineEventType resolvedType = extractEventType(eventType);
        return engineEventService.listPresets(resolvedType).stream()
                .map(this::toPresetResponse)
                .toList();
    }

    @PostMapping("/event-presets")
    public Map<String, Object> saveEventPreset(@RequestBody Map<String, Object> request) {
        EngineEventType eventType = extractEventType(request.get("eventType"));

        EventPreset preset = new EventPreset();
        preset.setName(extractRequiredString(request.get("name"), "name"));
        preset.setEventType(eventType);
        preset.setEventName(extractRequiredString(request.get("eventName"), "eventName"));
        preset.setCorrelationKey(extractOptionalString(request.get("correlationKey")));
        preset.setMessageId(extractOptionalString(request.get("messageId")));
        preset.setTimeToLiveMs(extractOptionalLong(request.get("timeToLiveMs")));
        preset.setPayloadJson(extractPayloadJson(request.get("payloadJson"), request.get("payloadText")));

        if (eventType == EngineEventType.MESSAGE && preset.getCorrelationKey() == null) {
            throw new IllegalArgumentException("correlationKey is required for message presets");
        }

        EventPreset saved = engineEventService.savePreset(preset);
        return toPresetResponse(saved);
    }

    @DeleteMapping("/event-presets/{id}")
    public Map<String, Object> deleteEventPreset(@PathVariable Long id) {
        engineEventService.deletePreset(id);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/event-dispatches")
    public List<Map<String, Object>> listEventDispatches(@RequestParam(required = false) String eventType,
                                                         @RequestParam(defaultValue = "50") int limit) {
        EngineEventType resolvedType = eventType == null || eventType.isBlank() ? null : extractEventType(eventType);
        return engineEventService.listDispatches(resolvedType, limit).stream()
                .map(this::toDispatchResponse)
                .toList();
    }

    @DeleteMapping("/event-dispatches")
    public Map<String, Object> clearEventDispatches(@RequestParam(required = false) String eventType) {
        EngineEventType resolvedType = eventType == null || eventType.isBlank() ? null : extractEventType(eventType);
        long deleted = engineEventService.clearDispatches(resolvedType);
        return Map.of("deleted", deleted);
    }

    private Map<String, Object> toPresetResponse(EventPreset preset) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", preset.getId());
        response.put("name", preset.getName());
        response.put("eventType", preset.getEventType() != null ? preset.getEventType().name() : null);
        response.put("eventName", preset.getEventName());
        response.put("correlationKey", preset.getCorrelationKey());
        response.put("messageId", preset.getMessageId());
        response.put("timeToLiveMs", preset.getTimeToLiveMs());
        response.put("payloadText", preset.getPayloadJson());
        response.put("createdAt", preset.getCreatedAt());
        response.put("updatedAt", preset.getUpdatedAt());
        return response;
    }

    private Map<String, Object> toDispatchResponse(EventDispatchAudit dispatch) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", dispatch.getId());
        response.put("eventType", dispatch.getEventType() != null ? dispatch.getEventType().name() : null);
        response.put("name", dispatch.getEventName());
        response.put("correlationKey", dispatch.getCorrelationKey());
        response.put("success", dispatch.isSuccess());
        response.put("detail", dispatch.isSuccess() ? "accepted" : dispatch.getErrorMessage());
        response.put("responseJson", dispatch.getResponseJson());
        response.put("payloadJson", dispatch.getRequestPayloadJson());
        response.put("time", dispatch.getDispatchedAt());
        return response;
    }

    private EngineEventType extractEventType(Object value) {
        String raw = extractRequiredString(value, "eventType");
        try {
            return EngineEventType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("eventType must be either MESSAGE or SIGNAL");
        }
    }

    private String extractRequiredString(Object value, String fieldName) {
        String result = extractOptionalString(value);
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return result;
    }

    private String extractOptionalString(Object value) {
        if (value == null) {
            return null;
        }
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private Long extractOptionalLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("timeToLiveMs must be a valid number");
        }
    }

    private Map<String, Object> extractVariablesMap(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return converted;
        }
        throw new IllegalArgumentException("variables must be a JSON object");
    }

    private String extractPayloadJson(Object payloadJson, Object payloadText) {
        String value = extractOptionalString(payloadJson);
        if (value != null) {
            return value;
        }
        value = extractOptionalString(payloadText);
        return value == null ? "{}" : value;
    }
}
