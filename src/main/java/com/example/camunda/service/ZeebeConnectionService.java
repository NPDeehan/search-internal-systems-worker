package com.example.camunda.service;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.BroadcastSignalCommandStep1;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.response.BroadcastSignalResponse;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ZeebeConnectionService {
    
    private static final Logger log = LoggerFactory.getLogger(ZeebeConnectionService.class);
    
    private final ZeebeClient zeebeClient;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public ZeebeConnectionService(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    public boolean isConnected() {
        try {
            // Try a lightweight request to check connection
            zeebeClient.newActivateJobsCommand()
                    .jobType("health-check")
                    .maxJobsToActivate(1)
                    .timeout(Duration.ofSeconds(2))
                    .send()
                    .join();
            
            isConnected.set(true);
            lastError.set(null);
            log.debug("Zeebe connection check: SUCCESS");
            return true;
            
        } catch (Exception e) {
            isConnected.set(false);
            lastError.set(e.getMessage());
            log.warn("Zeebe connection check: FAILED - {}", e.getMessage());
            return false;
        }
    }

    public String getLastError() {
        return lastError.get();
    }

    public boolean getCachedConnectionStatus() {
        return isConnected.get();
    }

    public ZeebeClient getClient() {
        return zeebeClient;
    }

    public Map<String, Object> publishMessage(String messageName,
                                              String correlationKey,
                                              Map<String, Object> variables,
                                              String messageId,
                                              Long timeToLiveMs) {
        PublishMessageCommandStep1.PublishMessageCommandStep3 command = zeebeClient
                .newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey(correlationKey)
                .variables(variables == null ? Map.of() : variables);

        if (messageId != null && !messageId.trim().isEmpty()) {
            command = command.messageId(messageId.trim());
        }
        if (timeToLiveMs != null && timeToLiveMs > 0) {
            command = command.timeToLive(Duration.ofMillis(timeToLiveMs));
        }

        PublishMessageResponse response = command.send().join();

        Map<String, Object> result = new HashMap<>();
        result.put("messageKey", response.getMessageKey());
        result.put("tenantId", response.getTenantId());
        return result;
    }

    public Map<String, Object> broadcastSignal(String signalName, Map<String, Object> variables) {
        BroadcastSignalCommandStep1.BroadcastSignalCommandStep2 command = zeebeClient
                .newBroadcastSignalCommand()
                .signalName(signalName)
                .variables(variables == null ? Map.of() : variables);

        BroadcastSignalResponse response = command.send().join();

        Map<String, Object> result = new HashMap<>();
        result.put("signalKey", response.getKey());
        result.put("tenantId", response.getTenantId());
        return result;
    }
}
