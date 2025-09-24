package com.example.camunda;

import com.example.camunda.config.CamundaProperties;
import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ZeebeConfig {
    
    private final CamundaProperties camundaProperties;

    @Bean
    public ZeebeClient zeebeClient() {
        log.info("Configuring Zeebe client for cluster: {}", 
                camundaProperties.getCloud().getClusterId());
        
        return ZeebeClient.newCloudClientBuilder()
            .withClusterId(camundaProperties.getCloud().getClusterId())
            .withClientId(camundaProperties.getAuth().getClientId())
            .withClientSecret(camundaProperties.getAuth().getClientSecret())
            .withRegion(camundaProperties.getCloud().getRegion())
            .build();
    }
}
