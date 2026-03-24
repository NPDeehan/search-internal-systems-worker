package com.example.camunda.service;

import com.example.camunda.exception.AccountNotFoundException;
import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.repository.AccountRepository;
import com.example.camunda.repository.CustomerAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final CustomerAccountRepository customerAccountRepository;

    public AccountService(AccountRepository accountRepository, CustomerAccountRepository customerAccountRepository) {
        this.accountRepository = accountRepository;
        this.customerAccountRepository = customerAccountRepository;
    }

    public List<AccountMatch> searchAccounts(Long accountId, Long customerId) {
        return searchAccounts(accountId, null, customerId);
    }

    public List<AccountMatch> searchAccounts(Long accountId, String accountNumber, Long customerId) {
        log.debug("Searching accounts - accountId: {}, customerId: {}", accountId, customerId);

        if (accountId == null && (accountNumber == null || accountNumber.trim().isEmpty()) && customerId == null) {
            log.warn("No search parameters provided for account lookup");
            return List.of();
        }

        Optional<Account> resolvedAccount = resolveAccount(accountId, accountNumber);
        Long resolvedAccountId = resolvedAccount.map(Account::getAccountId).orElse(accountId);

        if (resolvedAccountId != null && customerId != null) {
            Optional<CustomerAccount> link = customerAccountRepository
                    .findByCustomer_CustomerIdAndAccount_AccountId(customerId, resolvedAccountId);
            if (link.isEmpty()) {
                log.info("No account link found for accountId {} and customerId {}", resolvedAccountId, customerId);
                return List.of();
            }
            Optional<Account> accountOpt = accountRepository.findByAccountId(resolvedAccountId);
            if (accountOpt.isEmpty()) {
                log.warn("Account {} referenced in link but not present", resolvedAccountId);
                return List.of();
            }
            Account account = accountOpt.get();
            List<CustomerAccount> linksForAccount = customerAccountRepository.findByAccount_AccountId(resolvedAccountId);
            return List.of(buildAccountMatch(account, linksForAccount));
        }

        if (resolvedAccountId != null) {
            Optional<Account> accountOpt = accountRepository.findByAccountId(resolvedAccountId);
            if (accountOpt.isEmpty()) {
                log.info("No account found with ID {}", resolvedAccountId);
                return List.of();
            }
            Account account = accountOpt.get();
            List<CustomerAccount> linksForAccount = customerAccountRepository.findByAccount_AccountId(resolvedAccountId);
            return List.of(buildAccountMatch(account, linksForAccount));
        }

        // customerId only
        List<CustomerAccount> customerLinks = customerAccountRepository.findByCustomer_CustomerId(customerId);
        if (customerLinks.isEmpty()) {
            log.info("No accounts linked to customerId {}", customerId);
            return List.of();
        }

        Map<Long, List<CustomerAccount>> byAccount = new LinkedHashMap<>();
        for (CustomerAccount link : customerLinks) {
            if (link.getAccount() == null) {
                log.warn("Skipping link {} because account is null", link.getId());
                continue;
            }
            byAccount.computeIfAbsent(link.getAccount().getAccountId(), k -> new ArrayList<>()).add(link);
        }

        List<AccountMatch> matches = new ArrayList<>();
        for (Map.Entry<Long, List<CustomerAccount>> entry : byAccount.entrySet()) {
            Long resolvedId = entry.getKey();
            Optional<Account> accountOpt = accountRepository.findByAccountId(resolvedId);
            if (accountOpt.isEmpty()) {
                log.warn("Account {} referenced in customer links but not present", resolvedId);
                continue;
            }
            Account account = accountOpt.get();
            matches.add(buildAccountMatch(account, entry.getValue()));
        }

        return matches;
    }

    private Optional<Account> resolveAccount(Long accountId, String accountNumber) {
        if (accountId != null) {
            Optional<Account> byId = accountRepository.findByAccountId(accountId);
            if (byId.isPresent()) {
                return byId;
            }

            // Fallback: allow requests like 1001 to match account numbers such as SA-1001-...
            Optional<Account> byToken = accountRepository.findFirstByAccountNumberContainingOrderByAccountIdAsc(accountId.toString());
            if (byToken.isPresent()) {
                return byToken;
            }
        }

        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            String trimmed = accountNumber.trim();
            Optional<Account> byExactNumber = accountRepository.findByAccountNumber(trimmed);
            if (byExactNumber.isPresent()) {
                return byExactNumber;
            }
            return accountRepository.findFirstByAccountNumberContainingOrderByAccountIdAsc(trimmed);
        }

        return Optional.empty();
    }

    private AccountMatch buildAccountMatch(Account account, List<CustomerAccount> links) {
        List<RoleEntry> roles = new ArrayList<>();
        for (CustomerAccount link : links) {
            if (link.getCustomer() == null || link.getRole() == null) {
                continue;
            }
            roles.add(new RoleEntry(
                    link.getCustomer().getCustomerId(),
                    link.getCustomer().getCustomerName(),
                    link.getRole()));
        }

        // If there were no links, still return account with empty roles for completeness
        return new AccountMatch(account, roles);
    }

    public record AccountMatch(Account account, List<RoleEntry> roles) { }
    public record RoleEntry(Long customerId, String customerName, AccountRole role) { }

    public List<AccountMatch> getAllAccountsWithRoles() {
        List<Account> accounts = accountRepository.findAll();
        List<AccountMatch> matches = new ArrayList<>();
        for (Account account : accounts) {
            List<CustomerAccount> links = customerAccountRepository.findByAccount_AccountId(account.getAccountId());
            matches.add(buildAccountMatch(account, links));
        }
        return matches;
    }

    public List<CustomerAccount> getAllCustomerAccountLinks() {
        return customerAccountRepository.findAllWithCustomerAndAccount();
    }

    public Optional<Account> findAccountById(Long accountId) {
        log.debug("Finding account by ID: {}", accountId);
        if (accountId == null) {
            return Optional.empty();
        }
        return accountRepository.findById(accountId);
    }

    public Account getAccountById(Long accountId) {
        return findAccountById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
    }

    @Transactional
    public Account saveAccount(Account account) {
        log.info("Saving account: {}", account != null ? account.getAccountNumber() : null);
        if (account != null && account.getAccountId() == null) {
            Long maxAccountId = accountRepository.findMaxAccountId();
            long nextAccountId = (maxAccountId == null ? 0L : maxAccountId) + 1L;
            account.setAccountId(nextAccountId);
            log.info("Assigned generated accountId {} for new account {}", nextAccountId, account.getAccountNumber());
        }
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        log.info("Deleting account with ID: {}", accountId);
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }
        List<CustomerAccount> links = customerAccountRepository.findByAccount_AccountId(accountId);
        if (!links.isEmpty()) {
            log.info("Removing {} customer-account link(s) for account ID {} before account delete", links.size(), accountId);
            customerAccountRepository.deleteAll(links);
        }
        accountRepository.deleteById(accountId);
    }
}
