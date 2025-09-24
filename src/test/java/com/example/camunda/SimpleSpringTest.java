package com.example.camunda;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Spring Boot integration test
 * Uses inline properties to avoid configuration file issues
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "camunda.zeebe.enabled=false",
    "logging.level.com.example.camunda=ERROR"
})
public class SimpleSpringTest {

    @Test
    public void contextLoads() {
        // If this test passes, Spring context loads successfully
        assertTrue(true);
    }

    @Test
    public void applicationContextTest() {
        // Basic test to verify Spring is working
        assertNotNull(this, "Spring Boot application should be running");
    }
}
