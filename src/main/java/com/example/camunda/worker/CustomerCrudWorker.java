package com.example.camunda.worker;

import com.example.camunda.exception.CustomerNotFoundException;
import com.example.camunda.model.Customer;
import com.example.camunda.model.TrustLevel;
import com.example.camunda.service.CustomerService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(CustomerCrudWorker.class);

    private final CustomerService customerService;

    public CustomerCrudWorker(CustomerService customerService) {
        this.customerService = customerService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-customer-record job: {}", job.getKey());

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
            log.error("Error processing customer {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long customerId = extractLong(variables.get("customerId"));
        String customerName = extractString(variables.get("customerName"));
        String address = extractString(variables.get("address"));
        String email = extractString(variables.get("email"));
        Long employeeId = extractLong(variables.get("employeeId"));
        TrustLevel trustLevel = extractTrustLevel(variables.get("trustLevel"));

        if (email != null && !isValidEmail(email)) {
            return buildValidationError("email must be a valid email address", "UPDATE");
        }

        String validationMessage = validateCreateInput(customerName, address, email, employeeId, trustLevel);
        if (validationMessage != null) {
            return buildValidationError(validationMessage, "CREATE");
        }

        if (customerId != null && customerExists(customerId)) {
            return buildValidationError("Customer already exists with ID: " + customerId, "CREATE");
        }

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setCustomerName(customerName);
        customer.setAddress(address);
        customer.setEmail(email);
        customer.setEmployeeId(employeeId);
        customer.setTrustLevel(trustLevel);

        Customer saved = customerService.saveCustomer(customer);
        return buildSuccessResult("CREATE", saved, "Customer created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long customerId = extractLong(variables.get("customerId"));
        if (customerId == null) {
            return buildValidationError("customerId is required for UPDATE operation", "UPDATE");
        }

        Customer customer;
        try {
            customer = customerService.getCustomerById(customerId);
        } catch (CustomerNotFoundException ex) {
            return buildNotFoundResult("UPDATE", customerId);
        }

        String customerName = extractString(variables.get("customerName"));
        String address = extractString(variables.get("address"));
        String email = extractString(variables.get("email"));
        Long employeeId = extractLong(variables.get("employeeId"));
        TrustLevel trustLevel = extractTrustLevel(variables.get("trustLevel"));

        if (customerName != null) customer.setCustomerName(customerName);
        if (address != null) customer.setAddress(address);
        if (email != null) customer.setEmail(email);
        if (employeeId != null) customer.setEmployeeId(employeeId);
        if (trustLevel != null) customer.setTrustLevel(trustLevel);

        Customer saved = customerService.saveCustomer(customer);
        return buildSuccessResult("UPDATE", saved, "Customer updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long customerId = extractLong(variables.get("customerId"));
        if (customerId == null) {
            return buildValidationError("customerId is required for DELETE operation", "DELETE");
        }

        if (!customerExists(customerId)) {
            return buildNotFoundResult("DELETE", customerId);
        }

        customerService.deleteCustomer(customerId);

        Map<String, Object> customerCrudResult = new HashMap<>();
        customerCrudResult.put("status", "SUCCESS");
        customerCrudResult.put("operation", "DELETE");
        customerCrudResult.put("message", "Customer deleted successfully");
        customerCrudResult.put("customerId", customerId);
        customerCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerCrudResult", customerCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("customerId", customerId);
        result.put("customerName", null);

        return result;
    }

    private boolean customerExists(Long customerId) {
        try {
            customerService.getCustomerById(customerId);
            return true;
        } catch (CustomerNotFoundException ex) {
            return false;
        }
    }

    private String validateCreateInput(String customerName,
                                       String address,
                                       String email,
                                       Long employeeId,
                                       TrustLevel trustLevel) {
        if (customerName == null) return "customerName is required for CREATE operation";
        if (address == null) return "address is required for CREATE operation";
        if (email == null) return "email is required for CREATE operation";
        if (!isValidEmail(email)) return "email must be a valid email address";
        if (employeeId == null) return "employeeId is required for CREATE operation";
        if (trustLevel == null) return "trustLevel is required for CREATE operation (L1, L2, L3)";
        return null;
    }

    private Map<String, Object> buildSuccessResult(String operation, Customer customer, String message) {
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerId", customer.getCustomerId());
        customerData.put("customerName", customer.getCustomerName());
        customerData.put("address", customer.getAddress());
        customerData.put("email", customer.getEmail());
        customerData.put("employeeId", customer.getEmployeeId());
        customerData.put("trustLevel", customer.getTrustLevel() != null ? customer.getTrustLevel().name() : null);

        Map<String, Object> customerCrudResult = new HashMap<>();
        customerCrudResult.put("status", "SUCCESS");
        customerCrudResult.put("operation", operation);
        customerCrudResult.put("message", message);
        customerCrudResult.put("customer", customerData);
        customerCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerCrudResult", customerCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("customerId", customer.getCustomerId());
        result.put("customerName", customer.getCustomerName());
        result.put("customerEmail", customer.getEmail());

        return result;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long customerId) {
        Map<String, Object> customerCrudResult = new HashMap<>();
        customerCrudResult.put("status", "NOT_FOUND");
        customerCrudResult.put("operation", operation);
        customerCrudResult.put("message", "Customer not found with ID: " + customerId);
        customerCrudResult.put("customerId", customerId);
        customerCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerCrudResult", customerCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("customerId", customerId);
        result.put("customerName", null);
        result.put("customerEmail", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> customerCrudResult = new HashMap<>();
        customerCrudResult.put("status", "VALIDATION_ERROR");
        customerCrudResult.put("operation", operation);
        customerCrudResult.put("message", message);
        customerCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerCrudResult", customerCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("customerId", null);
        result.put("customerName", null);
        result.put("customerEmail", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> customerCrudResult = new HashMap<>();
        customerCrudResult.put("status", "ERROR");
        customerCrudResult.put("operation", operation);
        customerCrudResult.put("message", "An unexpected error occurred while processing customer operation");
        customerCrudResult.put("errorDetails", errorMessage);
        customerCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("customerCrudResult", customerCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("customerId", null);
        result.put("customerName", null);
        result.put("customerEmail", null);

        return result;
    }

    private boolean isValidEmail(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
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

    private TrustLevel extractTrustLevel(Object value) {
        String trustLevel = extractString(value);
        if (trustLevel == null) {
            return null;
        }
        try {
            return TrustLevel.valueOf(trustLevel.toUpperCase());
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
