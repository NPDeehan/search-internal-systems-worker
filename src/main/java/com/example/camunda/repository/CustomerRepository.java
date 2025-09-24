package com.example.camunda.repository;

import com.example.camunda.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(Long customerId);
    Optional<Customer> findByCustomerName(String customerName);
    Optional<Customer> findByCustomerIdOrCustomerName(Long customerId, String customerName);
}
