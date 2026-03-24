package com.example.camunda.service;

import com.example.camunda.exception.AccountNotFoundException;
import com.example.camunda.exception.CustomerNotFoundException;
import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.Customer;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.repository.AccountRepository;
import com.example.camunda.repository.CustomerAccountRepository;
import com.example.camunda.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerAccountLinkService {

    private static final Logger log = LoggerFactory.getLogger(CustomerAccountLinkService.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAccountRepository customerAccountRepository;

    public CustomerAccountLinkService(AccountRepository accountRepository,
                                      CustomerRepository customerRepository,
                                      CustomerAccountRepository customerAccountRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.customerAccountRepository = customerAccountRepository;
    }

    @Transactional
    public UpsertResult upsertRole(Long accountId, Long customerId, AccountRole role) {
        log.info("Upserting customer-account role: accountId={}, customerId={}, role={}", accountId, customerId, role);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        Optional<CustomerAccount> existing = customerAccountRepository
                .findByCustomer_CustomerIdAndAccount_AccountId(customerId, accountId);

        if (existing.isPresent()) {
            CustomerAccount link = existing.get();
            link.setRole(role);
            CustomerAccount saved = customerAccountRepository.save(link);
            return new UpsertResult(saved, false);
        }

        CustomerAccount created = new CustomerAccount();
        created.setAccount(account);
        created.setCustomer(customer);
        created.setRole(role);

        CustomerAccount saved = customerAccountRepository.save(created);
        return new UpsertResult(saved, true);
    }

    @Transactional
    public boolean removeLink(Long accountId, Long customerId) {
        log.info("Removing customer-account link: accountId={}, customerId={}", accountId, customerId);

        Optional<CustomerAccount> existing = customerAccountRepository
                .findByCustomer_CustomerIdAndAccount_AccountId(customerId, accountId);

        if (existing.isEmpty()) {
            return false;
        }

        customerAccountRepository.delete(existing.get());
        return true;
    }

    public record UpsertResult(CustomerAccount link, boolean created) {
    }
}
