package com.example.camunda.service;

import com.example.camunda.model.Customer;
import com.example.camunda.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BasicServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
        assertThat(customerService).isNotNull();
        assertThat(customerRepository).isNotNull();
    }

    @Test
    void testGetAllCustomers() {
        // Test basic service functionality
        List<Customer> customers = customerService.getAllCustomers();
        assertThat(customers).isNotNull();
    }

    @Test
    void testSaveAndFindCustomer() {
        // Create and save a test customer
        Customer customer = new Customer();
        customer.setCustomerId(100L);
        customer.setCustomerName("Test Customer");
        customer.setEmployeeId(1L);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        Customer saved = customerService.saveCustomer(customer);
        assertThat(saved).isNotNull();
        assertThat(saved.getCustomerName()).isEqualTo("Test Customer");

        // Test finding the customer
        Optional<Customer> found = customerService.findCustomer(100L, null);
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("Test Customer");
    }
}
