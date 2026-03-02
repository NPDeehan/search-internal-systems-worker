package com.example.camunda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "camunda.client")
@Validated
public class CamundaProperties {
    
    @NotBlank(message = "Camunda client mode is required")
    private String mode = "saas";
    
    private Auth auth = new Auth();
    private Cloud cloud = new Cloud();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }
    
    public static class Auth {
        @NotBlank(message = "Client ID is required")
        private String clientId;
        
        @NotBlank(message = "Client secret is required")
        private String clientSecret;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
    
    public static class Cloud {
        @NotBlank(message = "Cluster ID is required")
        private String clusterId;
        
        @NotBlank(message = "Region is required")
        private String region;

        public String getClusterId() {
            return clusterId;
        }

        public void setClusterId(String clusterId) {
            this.clusterId = clusterId;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
