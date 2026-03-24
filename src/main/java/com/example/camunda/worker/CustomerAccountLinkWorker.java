package com.example.camunda.worker;

import com.example.camunda.exception.AccountNotFoundException;
import com.example.camunda.exception.CustomerNotFoundException;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.service.CustomerAccountLinkService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerAccountLinkWorker {

    private static final Logger log = LoggerFactory.getLogger(CustomerAccountLinkWorker.class);

    private final CustomerAccountLinkService customerAccountLinkService;

    public CustomerAccountLinkWorker(CustomerAccountLinkService customerAccountLinkService) {
        this.customerAccountLinkService = customerAccountLinkService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-customer-account-link job: {}", job.getKey());

        Map<String, Object> variables = job.getVariablesAsMap();
        String operation = normalizeOperation(variables.get("operation"));

        if (operation == null) {
            return buildValidationError("Operation must be one of UPSERT_ROLE or REMOVE_LINK", null);
        }

        try {
            return switch (operation) {
                case "UPSERT_ROLE" -> handleUpsertRole(variables);
                case "REMOVE_LINK" -> handleRemoveLink(variables);
                default -> buildValidationError("Unsupported operation: " + operation, operation);
            };
        } catch (AccountNotFoundException | CustomerNotFoundException ex) {
            return buildNotFoundResult(operation, ex.getMessage(), extractLong(variables.get("accountId")), extractLong(variables.get("customerId")));
        } catch (Exception e) {
            log.error("Error processing customer-account-link {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    private Map<String, Object> handleUpsertRole(Map<String, Object> variables) {
        Long accountId = extractLong(variables.get("accountId"));
        Long customerId = extractLong(variables.get("customerId"));
        AccountRole role = extractAccountRole(variables.get("role"));

        if (accountId == null) {
            return buildValidationError("accountId is required for UPSERT_ROLE operation", "UPSERT_ROLE");
        }
        if (customerId == null) {
            return buildValidationError("customerId is required for UPSERT_ROLE operation", "UPSERT_ROLE");
        }
        if (role == null) {
            return buildValidationError("role is required for UPSERT_ROLE operation (OWNER, ADMIN, NAMED)", "UPSERT_ROLE");
        }

        CustomerAccountLinkService.UpsertResult upsertResult = customerAccountLinkService.upsertRole(accountId, customerId, role);
        CustomerAccount link = upsertResult.link();

        String message = upsertResult.created()
                ? "Customer-account link created successfully"
                : "Customer-account role updated successfully";

        return buildSuccessResult("UPSERT_ROLE", link, message, upsertResult.created() ? "CREATED" : "UPDATED");
    }

    private Map<String, Object> handleRemoveLink(Map<String, Object> variables) {
        Long accountId = extractLong(variables.get("accountId"));
        Long customerId = extractLong(variables.get("customerId"));

        if (accountId == null) {
            return buildValidationError("accountId is required for REMOVE_LINK operation", "REMOVE_LINK");
        }
        if (customerId == null) {
            return buildValidationError("customerId is required for REMOVE_LINK operation", "REMOVE_LINK");
        }

        boolean removed = customerAccountLinkService.removeLink(accountId, customerId);
        if (!removed) {
            return buildNotFoundResult("REMOVE_LINK", "Customer-account link not found", accountId, customerId);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "SUCCESS");
        payload.put("operation", "REMOVE_LINK");
        payload.put("message", "Customer-account link removed successfully");
        payload.put("action", "REMOVED");
        payload.put("accountId", accountId);
        payload.put("customerId", customerId);
        payload.put("role", null);
        payload.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerAccountLinkResult", payload);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "REMOVE_LINK");
        result.put("accountId", accountId);
        result.put("customerId", customerId);
        result.put("role", null);

        return result;
    }

    private Map<String, Object> buildSuccessResult(String operation,
                                                   CustomerAccount link,
                                                   String message,
                                                   String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "SUCCESS");
        payload.put("operation", operation);
        payload.put("message", message);
        payload.put("action", action);
        payload.put("linkId", link.getId());
        payload.put("accountId", link.getAccount() != null ? link.getAccount().getAccountId() : null);
        payload.put("customerId", link.getCustomer() != null ? link.getCustomer().getCustomerId() : null);
        payload.put("role", link.getRole() != null ? link.getRole().name() : null);
        payload.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerAccountLinkResult", payload);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("accountId", link.getAccount() != null ? link.getAccount().getAccountId() : null);
        result.put("customerId", link.getCustomer() != null ? link.getCustomer().getCustomerId() : null);
        result.put("role", link.getRole() != null ? link.getRole().name() : null);

        return result;
    }

    private Map<String, Object> buildNotFoundResult(String operation, String message, Long accountId, Long customerId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "NOT_FOUND");
        payload.put("operation", operation);
        payload.put("message", message);
        payload.put("accountId", accountId);
        payload.put("customerId", customerId);
        payload.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerAccountLinkResult", payload);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("accountId", accountId);
        result.put("customerId", customerId);
        result.put("role", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "VALIDATION_ERROR");
        payload.put("operation", operation);
        payload.put("message", message);
        payload.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerAccountLinkResult", payload);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("accountId", null);
        result.put("customerId", null);
        result.put("role", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "ERROR");
        payload.put("operation", operation);
        payload.put("message", "An unexpected error occurred while processing customer-account link operation");
        payload.put("errorDetails", errorMessage);
        payload.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerAccountLinkResult", payload);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("accountId", null);
        result.put("customerId", null);
        result.put("role", null);

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

    private AccountRole extractAccountRole(Object value) {
        String role = extractString(value);
        if (role == null) {
            return null;
        }
        try {
            return AccountRole.valueOf(role.toUpperCase());
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
