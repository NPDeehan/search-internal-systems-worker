package com.example.camunda.repository;

import com.example.camunda.model.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    List<CustomerAccount> findByCustomer_CustomerId(Long customerId);
    List<CustomerAccount> findByAccount_AccountId(Long accountId);
    Optional<CustomerAccount> findByCustomer_CustomerIdAndAccount_AccountId(Long customerId, Long accountId);

    @Query("SELECT ca FROM CustomerAccount ca JOIN FETCH ca.customer JOIN FETCH ca.account")
    List<CustomerAccount> findAllWithCustomerAndAccount();
}
