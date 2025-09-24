package com.example.camunda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // This enables JPA auditing features like @CreatedDate and @LastModifiedDate
}
