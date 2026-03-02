package com.example.camunda;

import com.example.camunda.config.CamundaProperties;
import io.camunda.zeebe.client.ZeebeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZeebeConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ZeebeConfig.class);
    
    private final CamundaProperties camundaProperties;

    public ZeebeConfig(CamundaProperties camundaProperties) {
        this.camundaProperties = camundaProperties;
    }

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
