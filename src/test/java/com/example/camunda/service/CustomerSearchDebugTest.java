package com.example.camunda.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.camunda.model.Customer;
import com.example.camunda.exception.CustomerNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
class CustomerSearchDebugTest {

    @Autowired
    private CustomerService customerService;

    @Test
    void testAvailableCustomers() {
        // Get all customers to see what's actually in the database
        List<Customer> allCustomers = customerService.getAllCustomers();
        log.info("Total customers in database: {}", allCustomers.size());
        
        // Print first 10 customers for debugging
        allCustomers.stream()
                .limit(10)
                .forEach(customer -> log.info("Customer: ID={}, Name='{}'", 
                        customer.getCustomerId(), customer.getCustomerName()));
    }

    @Test
    void testSearchByExistingCustomerName() {
        // Get all customers
        List<Customer> allCustomers = customerService.getAllCustomers();
        assertFalse(allCustomers.isEmpty(), "Database should contain customers");

        // Try to search for the first customer by name
        Customer firstCustomer = allCustomers.get(0);
        log.info("Searching for customer: '{}'", firstCustomer.getCustomerName());

        Optional<Customer> foundCustomer = customerService.findCustomer(null, firstCustomer.getCustomerName());
        assertTrue(foundCustomer.isPresent(), "Should find customer by exact name match");
        assertEquals(firstCustomer.getCustomerId(), foundCustomer.get().getCustomerId());
    }

    @Test
    void testSearchByNonExistentCustomerName() {
        Optional<Customer> foundCustomer = customerService.findCustomer(null, "NonExistentCustomer XYZ123");
        assertFalse(foundCustomer.isPresent(), "Should not find non-existent customer");

        foundCustomer = customerService.findCustomer(null, "This Customer Definitely Does Not Exist");
        assertFalse(foundCustomer.isPresent(), "Should not find non-existent customer");
    }

    @Test
    void testPartialNameMatching() {
        // Get all customers
        List<Customer> allCustomers = customerService.getAllCustomers();
        assertFalse(allCustomers.isEmpty(), "Database should contain customers");

        // Try partial matching with first customer
        Customer firstCustomer = allCustomers.get(0);
        String[] nameParts = firstCustomer.getCustomerName().split(" ");
        
        if (nameParts.length > 0) {
            String partialName = nameParts[0]; // First word of name
            log.info("Testing partial name search with: '{}'", partialName);
            
            Optional<Customer> foundCustomer = customerService.findCustomer(null, partialName);
            if (foundCustomer.isPresent()) {
                log.info("Partial match found: '{}' matched '{}'", partialName, foundCustomer.get().getCustomerName());
            } else {
                log.info("No partial match found for: '{}'", partialName);
            }
        }
    }

    @Test
    void testErrorHandlingForMissingCustomer() {
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerWithEmployee(null, "NonExistentCustomer");
        });
    }
}
