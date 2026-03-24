package com.example.camunda.repository;

import com.example.camunda.model.Account;
import com.example.camunda.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountId(Long accountId);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findFirstByAccountNumberContainingOrderByAccountIdAsc(String accountNumberToken);
    List<Account> findByAccountType(AccountType accountType);

    @Query("SELECT COALESCE(MAX(a.accountId), 0) FROM Account a")
    Long findMaxAccountId();
}
