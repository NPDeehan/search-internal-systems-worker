package com.example.camunda.service;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZeebeConnectionService {
    
    private final ZeebeClient zeebeClient;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicReference<String> lastError = new AtomicReference<>();

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
}
