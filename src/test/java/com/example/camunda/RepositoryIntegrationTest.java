package com.example.camunda;

import com.example.camunda.model.Customer;
import com.example.camunda.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for repository functionality
 */
@SpringBootTest
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
@Transactional
public class RepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    public void testCustomerRepositoryFunctionality() {
        // Test saving a customer
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerName("Test Customer");
        customer.setEmployeeId(1L);
        
        Customer savedCustomer = customerRepository.save(customer);
        
        // Verify save worked
        assertNotNull(savedCustomer);
        assertNotNull(savedCustomer.getCustomerId());
        assertEquals("Test Customer", savedCustomer.getCustomerName());
        assertEquals(1L, savedCustomer.getEmployeeId());
        
        // Test finding the customer
        assertTrue(customerRepository.findById(savedCustomer.getCustomerId()).isPresent());
        
        // Test count
        long count = customerRepository.count();
        assertTrue(count > 0);
    }

    @Test
    public void testRepositoryIsNotNull() {
        assertNotNull(customerRepository, "Customer repository should be injected");
    }
}
