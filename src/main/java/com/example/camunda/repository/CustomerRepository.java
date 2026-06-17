package com.example.camunda.repository;

import com.example.camunda.model.Customer;
import com.example.camunda.model.TrustLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(Long customerId);
    Optional<Customer> findByCustomerName(String customerName);
    Optional<Customer> findByCustomerIdOrCustomerName(Long customerId, String customerName);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByTrustLevel(TrustLevel trustLevel);
    List<Customer> findByCustomerNameContainingIgnoreCase(String customerName);

    @Query("SELECT COALESCE(MAX(c.customerId), 0) FROM Customer c")
    Long findMaxCustomerId();
}
