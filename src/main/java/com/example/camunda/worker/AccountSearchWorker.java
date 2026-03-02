package com.example.camunda.worker;

import com.example.camunda.model.Account;
import com.example.camunda.service.AccountService;
import com.example.camunda.service.AccountService.AccountMatch;
import com.example.camunda.service.AccountService.RoleEntry;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AccountSearchWorker {

    private static final Logger log = LoggerFactory.getLogger(AccountSearchWorker.class);
    private final AccountService accountService;

    public AccountSearchWorker(AccountService accountService) {
        this.accountService = accountService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing search-account job: {}", job.getKey());

        Map<String, Object> variables = job.getVariablesAsMap();
        log.debug("Raw variables received: {}", variables);

        Long accountId = extractLong(variables.get("accountId"));
        if (accountId == null) {
            accountId = extractLong(variables.get("customerAccountId"));
        }
        String accountNumber = extractString(variables.get("accountNumber"));
        Long customerId = extractLong(variables.get("customerId"));

        log.info("Searching accounts - accountId: {}, accountNumber: {}, customerId: {}", accountId, accountNumber, customerId);

        if (accountId == null && isEmpty(accountNumber) && customerId == null) {
            String message = "At least one search parameter (accountId/customerAccountId, accountNumber, or customerId) must be provided";
            log.error("Validation failed: {}", message);
            return buildValidationErrorResult(message, variables);
        }

        try {
            List<AccountMatch> matches = accountService.searchAccounts(accountId, accountNumber, customerId);

            boolean usedCustomerIdAsAccountIdFallback = false;
            Long fallbackAccountId = null;
            if (matches.isEmpty() && accountId == null && isEmpty(accountNumber) && customerId != null) {
                fallbackAccountId = customerId;
                matches = accountService.searchAccounts(fallbackAccountId, null, null);
                usedCustomerIdAsAccountIdFallback = !matches.isEmpty();
                if (usedCustomerIdAsAccountIdFallback) {
                    log.info("No result for customerId {} as customer. Matched as accountId fallback.", customerId);
                }
            }

            if (matches.isEmpty()) {
                log.info("No accounts found for accountId {} and customerId {}", accountId, customerId);
                return buildNotFoundResult(accountId, accountNumber, customerId, variables);
            }

            List<Map<String, Object>> accountMaps = convertMatchesToMaps(matches, customerId);
            Map<String, Object> result = new HashMap<>();

            Map<String, Object> searchResult = new HashMap<>();
            searchResult.put("status", "SUCCESS");
            searchResult.put("accountCount", matches.size());
            searchResult.put("accounts", accountMaps);
            searchResult.put("timestamp", java.time.LocalDateTime.now().toString());
            searchResult.put("searchParameters", buildSearchParams(accountId, accountNumber, customerId));
            if (usedCustomerIdAsAccountIdFallback) {
                searchResult.put("fallbackApplied", "customerId_interpreted_as_accountId");
                searchResult.put("resolvedAccountId", fallbackAccountId);
            }

            result.put("accountSearchResult", searchResult);
            result.put("searchStatus", "SUCCESS");
            result.put("accounts", accountMaps);
            result.put("accountCount", matches.size());

            if (matches.size() == 1) {
                AccountMatch match = matches.getFirst();
                Account account = match.account();
                result.put("accountId", account.getAccountId());
                result.put("accountNumber", account.getAccountNumber());
                result.put("accountType", account.getAccountType() != null ? account.getAccountType().name() : null);
                result.put("accountBalance", account.getBalance());
                result.put("accountCurrency", account.getCurrency());
                result.put("accountStatus", account.getStatus());
                result.put("accountInterestRate", account.getInterestRate());
                result.put("accountOpenedDate", formatDate(account.getOpenedDate()));
                result.put("accountLastActivityDate", formatDate(account.getLastActivityDate()));
                result.put("accountRole", findRoleForCustomer(match.roles(), customerId));
            } else {
                result.put("accountId", null);
                result.put("accountNumber", null);
                result.put("accountType", null);
                result.put("accountBalance", null);
                result.put("accountCurrency", null);
                result.put("accountStatus", null);
                result.put("accountInterestRate", null);
                result.put("accountOpenedDate", null);
                result.put("accountLastActivityDate", null);
                result.put("accountRole", null);
            }

            return result;
        } catch (Exception e) {
            log.error("Error occurred while searching accounts: {}", e.getMessage(), e);
            return buildErrorResult(e.getMessage(), variables, accountId, accountNumber, customerId);
        }
    }

    private Map<String, Object> convertMatchToMap(AccountMatch match, Long requestedCustomerId) {
        Map<String, Object> map = new HashMap<>();
        Account account = match.account();

        map.put("accountId", account.getAccountId());
        map.put("accountNumber", account.getAccountNumber());
        map.put("accountType", account.getAccountType() != null ? account.getAccountType().name() : null);
        map.put("balance", account.getBalance());
        map.put("currency", account.getCurrency());
        map.put("status", account.getStatus());
        map.put("interestRate", account.getInterestRate());
        map.put("openedDate", formatDate(account.getOpenedDate()));
        map.put("lastActivityDate", formatDate(account.getLastActivityDate()));

        List<Map<String, Object>> roles = new ArrayList<>();
        for (RoleEntry roleEntry : match.roles()) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("customerId", roleEntry.customerId());
            roleMap.put("customerName", roleEntry.customerName());
            roleMap.put("role", roleEntry.role() != null ? roleEntry.role().name() : null);
            roles.add(roleMap);
        }
        map.put("roles", roles);

        String primaryRole = findRoleForCustomer(match.roles(), requestedCustomerId);
        map.put("primaryRole", primaryRole);

        return map;
    }

    private List<Map<String, Object>> convertMatchesToMaps(List<AccountMatch> matches, Long requestedCustomerId) {
        return matches.stream()
                .map(match -> convertMatchToMap(match, requestedCustomerId))
                .toList();
    }

    private String findRoleForCustomer(List<RoleEntry> roles, Long customerId) {
        if (customerId == null) {
            return null;
        }
        return roles.stream()
                .filter(roleEntry -> roleEntry.customerId() != null && roleEntry.customerId().equals(customerId))
                .map(roleEntry -> roleEntry.role() != null ? roleEntry.role().name() : null)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> buildValidationErrorResult(String message, Map<String, Object> variables) {
        Map<String, Object> searchResult = new HashMap<>();
        searchResult.put("status", "VALIDATION_ERROR");
        searchResult.put("message", message);
        searchResult.put("timestamp", java.time.LocalDateTime.now().toString());
        searchResult.put("searchParameters", extractSearchParameters(variables));

        Map<String, Object> result = new HashMap<>();
        result.put("accountSearchResult", searchResult);
        result.put("searchStatus", "VALIDATION_ERROR");
        result.put("accounts", List.of());
        result.put("accountCount", 0);
        result.put("accountId", null);
        result.put("accountNumber", null);
        result.put("accountType", null);
        result.put("accountBalance", null);
        result.put("accountCurrency", null);
        result.put("accountStatus", null);
        result.put("accountInterestRate", null);
        result.put("accountOpenedDate", null);
        result.put("accountLastActivityDate", null);
        result.put("accountRole", null);
        return result;
    }

    private Map<String, Object> buildNotFoundResult(Long accountId, String accountNumber, Long customerId, Map<String, Object> variables) {
        Map<String, Object> searchResult = new HashMap<>();
        searchResult.put("status", "NOT_FOUND");
        searchResult.put("message", "No accounts could be found with the provided search criteria");
        searchResult.put("timestamp", java.time.LocalDateTime.now().toString());
        searchResult.put("searchParameters", buildSearchParams(accountId, accountNumber, customerId));

        Map<String, Object> result = new HashMap<>();
        result.put("accountSearchResult", searchResult);
        result.put("searchStatus", "NOT_FOUND");
        result.put("accounts", List.of());
        result.put("accountCount", 0);
        result.put("accountId", null);
        result.put("accountNumber", null);
        result.put("accountType", null);
        result.put("accountBalance", null);
        result.put("accountCurrency", null);
        result.put("accountStatus", null);
        result.put("accountInterestRate", null);
        result.put("accountOpenedDate", null);
        result.put("accountLastActivityDate", null);
        result.put("accountRole", null);
        return result;
    }

    private Map<String, Object> buildErrorResult(String errorMessage, Map<String, Object> variables, Long accountId, String accountNumber, Long customerId) {
        Map<String, Object> searchResult = new HashMap<>();
        searchResult.put("status", "ERROR");
        searchResult.put("message", "An error occurred during account search");
        searchResult.put("errorDetails", errorMessage);
        searchResult.put("timestamp", java.time.LocalDateTime.now().toString());
        searchResult.put("searchParameters", buildSearchParams(accountId, accountNumber, customerId));

        Map<String, Object> result = new HashMap<>();
        result.put("accountSearchResult", searchResult);
        result.put("searchStatus", "ERROR");
        result.put("accounts", List.of());
        result.put("accountCount", 0);
        result.put("accountId", null);
        result.put("accountNumber", null);
        result.put("accountType", null);
        result.put("accountBalance", null);
        result.put("accountCurrency", null);
        result.put("accountStatus", null);
        result.put("accountInterestRate", null);
        result.put("accountOpenedDate", null);
        result.put("accountLastActivityDate", null);
        result.put("accountRole", null);
        return result;
    }

    private Map<String, Object> buildSearchParams(Long accountId, String accountNumber, Long customerId) {
        Map<String, Object> params = new HashMap<>();
        if (accountId != null) params.put("accountId", accountId);
        if (!isEmpty(accountNumber)) params.put("accountNumber", accountNumber);
        if (customerId != null) params.put("customerId", customerId);
        return params;
    }

    private Map<String, Object> extractSearchParameters(Map<String, Object> variables) {
        Map<String, Object> params = new HashMap<>();
        Long accountId = extractLong(variables.get("accountId"));
        if (accountId == null) {
            accountId = extractLong(variables.get("customerAccountId"));
        }
        String accountNumber = extractString(variables.get("accountNumber"));
        Long customerId = extractLong(variables.get("customerId"));
        if (accountId != null) params.put("accountId", accountId);
        if (!isEmpty(accountNumber)) params.put("accountNumber", accountNumber);
        if (customerId != null) params.put("customerId", customerId);
        return params;
    }

    private String extractString(Object value) {
        if (value == null) return null;
        if (value instanceof String str) {
            String trimmed = str.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String str) {
            if (str.trim().isEmpty()) return null;
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse '{}' as Long", str);
                return null;
            }
        }
        return null;
    }
}
