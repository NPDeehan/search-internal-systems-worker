package com.example.camunda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Component
@ConfigurationProperties(prefix = "camunda.client")
@Validated
public class CamundaProperties {
    
    @NotBlank(message = "Camunda client mode is required")
    private String mode = "saas";
    
    private Auth auth = new Auth();
    private Cloud cloud = new Cloud();
    
    @Data
    public static class Auth {
        @NotBlank(message = "Client ID is required")
        private String clientId;
        
        @NotBlank(message = "Client secret is required")
        private String clientSecret;
    }
    
    @Data
    public static class Cloud {
        @NotBlank(message = "Cluster ID is required")
        private String clusterId;
        
        @NotBlank(message = "Region is required")
        private String region;
    }
}
