package com.example.camunda.worker;

import com.example.camunda.model.Account;
import com.example.camunda.model.AccountType;
import com.example.camunda.service.AccountService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class AccountCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(AccountCrudWorker.class);

    private final AccountService accountService;

    public AccountCrudWorker(AccountService accountService) {
        this.accountService = accountService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-account-record job: {}", job.getKey());

        Map<String, Object> variables = job.getVariablesAsMap();
        String operation = normalizeOperation(variables.get("operation"));

        if (operation == null) {
            return buildValidationError("Operation must be one of CREATE, UPDATE, DELETE", null);
        }

        try {
            return switch (operation) {
                case "CREATE" -> handleCreate(variables);
                case "UPDATE" -> handleUpdate(variables);
                case "DELETE" -> handleDelete(variables);
                default -> buildValidationError("Unsupported operation: " + operation, operation);
            };
        } catch (Exception e) {
            log.error("Error processing account {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long accountId = extractLong(variables.get("accountId"));
        String accountNumber = extractString(variables.get("accountNumber"));
        AccountType accountType = extractAccountType(variables.get("accountType"));
        BigDecimal balance = extractBigDecimal(variables.get("balance"));
        String currency = extractString(variables.get("currency"));
        String status = extractString(variables.get("status"));
        BigDecimal interestRate = extractBigDecimal(variables.get("interestRate"));
        LocalDate openedDate = extractDate(variables.get("openedDate"));
        LocalDate lastActivityDate = extractDate(variables.get("lastActivityDate"));

        String validationMessage = validateCreateInput(accountNumber, accountType, balance, currency, status);
        if (validationMessage != null) {
            return buildValidationError(validationMessage, "CREATE");
        }

        if (accountId != null && accountService.findAccountById(accountId).isPresent()) {
            return buildValidationError("Account already exists with ID: " + accountId, "CREATE");
        }

        Account account = new Account();
        account.setAccountId(accountId);
        account.setAccountNumber(accountNumber);
        account.setAccountType(accountType);
        account.setBalance(balance);
        account.setCurrency(currency);
        account.setStatus(status);
        account.setInterestRate(interestRate);
        account.setOpenedDate(openedDate);
        account.setLastActivityDate(lastActivityDate);

        Account saved = accountService.saveAccount(account);
        return buildSuccessResult("CREATE", saved, "Account created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long accountId = extractLong(variables.get("accountId"));
        if (accountId == null) {
            return buildValidationError("accountId is required for UPDATE operation", "UPDATE");
        }

        if (accountService.findAccountById(accountId).isEmpty()) {
            return buildNotFoundResult("UPDATE", accountId);
        }

        Account account = accountService.getAccountById(accountId);

        String accountNumber = extractString(variables.get("accountNumber"));
        AccountType accountType = extractAccountType(variables.get("accountType"));
        BigDecimal balance = extractBigDecimal(variables.get("balance"));
        String currency = extractString(variables.get("currency"));
        String status = extractString(variables.get("status"));
        BigDecimal interestRate = extractBigDecimal(variables.get("interestRate"));
        LocalDate openedDate = extractDate(variables.get("openedDate"));
        LocalDate lastActivityDate = extractDate(variables.get("lastActivityDate"));

        if (accountNumber != null) account.setAccountNumber(accountNumber);
        if (accountType != null) account.setAccountType(accountType);
        if (balance != null) account.setBalance(balance);
        if (currency != null) account.setCurrency(currency);
        if (status != null) account.setStatus(status);
        if (interestRate != null) account.setInterestRate(interestRate);
        if (openedDate != null) account.setOpenedDate(openedDate);
        if (lastActivityDate != null) account.setLastActivityDate(lastActivityDate);

        Account saved = accountService.saveAccount(account);
        return buildSuccessResult("UPDATE", saved, "Account updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long accountId = extractLong(variables.get("accountId"));
        if (accountId == null) {
            return buildValidationError("accountId is required for DELETE operation", "DELETE");
        }

        if (accountService.findAccountById(accountId).isEmpty()) {
            return buildNotFoundResult("DELETE", accountId);
        }

        accountService.deleteAccount(accountId);

        Map<String, Object> accountCrudResult = new HashMap<>();
        accountCrudResult.put("status", "SUCCESS");
        accountCrudResult.put("operation", "DELETE");
        accountCrudResult.put("message", "Account deleted successfully");
        accountCrudResult.put("accountId", accountId);
        accountCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("accountCrudResult", accountCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("accountId", accountId);
        result.put("accountNumber", null);

        return result;
    }

    private String validateCreateInput(String accountNumber,
                                       AccountType accountType,
                                       BigDecimal balance,
                                       String currency,
                                       String status) {
        if (accountNumber == null) return "accountNumber is required for CREATE operation";
        if (accountType == null) return "accountType is required for CREATE operation";
        if (balance == null) return "balance is required for CREATE operation";
        if (currency == null) return "currency is required for CREATE operation";
        if (status == null) return "status is required for CREATE operation";
        return null;
    }

    private Map<String, Object> buildSuccessResult(String operation, Account account, String message) {
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("accountId", account.getAccountId());
        accountData.put("accountNumber", account.getAccountNumber());
        accountData.put("accountType", account.getAccountType() != null ? account.getAccountType().name() : null);
        accountData.put("balance", account.getBalance());
        accountData.put("currency", account.getCurrency());
        accountData.put("status", account.getStatus());
        accountData.put("interestRate", account.getInterestRate());
        accountData.put("openedDate", account.getOpenedDate() != null ? account.getOpenedDate().toString() : null);
        accountData.put("lastActivityDate", account.getLastActivityDate() != null ? account.getLastActivityDate().toString() : null);

        Map<String, Object> accountCrudResult = new HashMap<>();
        accountCrudResult.put("status", "SUCCESS");
        accountCrudResult.put("operation", operation);
        accountCrudResult.put("message", message);
        accountCrudResult.put("account", accountData);
        accountCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("accountCrudResult", accountCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("accountId", account.getAccountId());
        result.put("accountNumber", account.getAccountNumber());

        return result;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long accountId) {
        Map<String, Object> accountCrudResult = new HashMap<>();
        accountCrudResult.put("status", "NOT_FOUND");
        accountCrudResult.put("operation", operation);
        accountCrudResult.put("message", "Account not found with ID: " + accountId);
        accountCrudResult.put("accountId", accountId);
        accountCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("accountCrudResult", accountCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("accountId", accountId);
        result.put("accountNumber", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> accountCrudResult = new HashMap<>();
        accountCrudResult.put("status", "VALIDATION_ERROR");
        accountCrudResult.put("operation", operation);
        accountCrudResult.put("message", message);
        accountCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("accountCrudResult", accountCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("accountId", null);
        result.put("accountNumber", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> accountCrudResult = new HashMap<>();
        accountCrudResult.put("status", "ERROR");
        accountCrudResult.put("operation", operation);
        accountCrudResult.put("message", "An unexpected error occurred while processing account operation");
        accountCrudResult.put("errorDetails", errorMessage);
        accountCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("accountCrudResult", accountCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("accountId", null);
        result.put("accountNumber", null);

        return result;
    }

    private String normalizeOperation(Object value) {
        String operation = extractString(value);
        return operation != null ? operation.toUpperCase() : null;
    }

    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal extractBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try {
                return new BigDecimal(trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private LocalDate extractDate(Object value) {
        String dateValue = extractString(value);
        if (dateValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateValue);
        } catch (Exception e) {
            return null;
        }
    }

    private AccountType extractAccountType(Object value) {
        String accountType = extractString(value);
        if (accountType == null) {
            return null;
        }
        try {
            return AccountType.valueOf(accountType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String extractString(Object value) {
        if (value == null) return null;
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
