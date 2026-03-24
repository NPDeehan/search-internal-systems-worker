package com.example.camunda.worker;

import com.example.camunda.model.ExternalCompany;
import com.example.camunda.service.CompanyService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CompanyCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(CompanyCrudWorker.class);

    private final CompanyService companyService;

    public CompanyCrudWorker(CompanyService companyService) {
        this.companyService = companyService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-company-record job: {}", job.getKey());

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
            log.error("Error processing company {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long companyId = extractLong(variables.get("companyId"));
        String companyName = extractString(variables.get("companyName"));
        String address = extractString(variables.get("address"));
        String contactPerson = extractString(variables.get("contactPerson"));
        String phoneNumber = extractString(variables.get("phoneNumber"));

        String validationMessage = validateCreateInput(companyName);
        if (validationMessage != null) {
            return buildValidationError(validationMessage, "CREATE");
        }

        if (companyId != null && companyService.findCompany(companyId, null).isPresent()) {
            return buildValidationError("Company already exists with ID: " + companyId, "CREATE");
        }

        ExternalCompany company = new ExternalCompany();
        company.setCompanyId(companyId);
        company.setCompanyName(companyName);
        company.setAddress(address);
        company.setContactPerson(contactPerson);
        company.setPhoneNumber(phoneNumber);

        ExternalCompany saved = companyService.saveCompany(company);
        return buildSuccessResult("CREATE", saved, "Company created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long companyId = extractLong(variables.get("companyId"));
        if (companyId == null) {
            return buildValidationError("companyId is required for UPDATE operation", "UPDATE");
        }

        if (companyService.findCompany(companyId, null).isEmpty()) {
            return buildNotFoundResult("UPDATE", companyId);
        }

        ExternalCompany company = companyService.getCompanyById(companyId);

        String companyName = extractString(variables.get("companyName"));
        String address = extractString(variables.get("address"));
        String contactPerson = extractString(variables.get("contactPerson"));
        String phoneNumber = extractString(variables.get("phoneNumber"));

        if (companyName != null) company.setCompanyName(companyName);
        if (address != null) company.setAddress(address);
        if (contactPerson != null) company.setContactPerson(contactPerson);
        if (phoneNumber != null) company.setPhoneNumber(phoneNumber);

        ExternalCompany saved = companyService.saveCompany(company);
        return buildSuccessResult("UPDATE", saved, "Company updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long companyId = extractLong(variables.get("companyId"));
        if (companyId == null) {
            return buildValidationError("companyId is required for DELETE operation", "DELETE");
        }

        if (companyService.findCompany(companyId, null).isEmpty()) {
            return buildNotFoundResult("DELETE", companyId);
        }

        companyService.deleteCompany(companyId);

        Map<String, Object> companyCrudResult = new HashMap<>();
        companyCrudResult.put("status", "SUCCESS");
        companyCrudResult.put("operation", "DELETE");
        companyCrudResult.put("message", "Company deleted successfully");
        companyCrudResult.put("companyId", companyId);
        companyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("companyCrudResult", companyCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("companyId", companyId);
        result.put("companyName", null);

        return result;
    }

    private String validateCreateInput(String companyName) {
        if (companyName == null) return "companyName is required for CREATE operation";
        return null;
    }

    private Map<String, Object> buildSuccessResult(String operation, ExternalCompany company, String message) {
        Map<String, Object> companyData = new HashMap<>();
        companyData.put("companyId", company.getCompanyId());
        companyData.put("companyName", company.getCompanyName());
        companyData.put("address", company.getAddress());
        companyData.put("contactPerson", company.getContactPerson());
        companyData.put("phoneNumber", company.getPhoneNumber());

        Map<String, Object> companyCrudResult = new HashMap<>();
        companyCrudResult.put("status", "SUCCESS");
        companyCrudResult.put("operation", operation);
        companyCrudResult.put("message", message);
        companyCrudResult.put("company", companyData);
        companyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("companyCrudResult", companyCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("companyId", company.getCompanyId());
        result.put("companyName", company.getCompanyName());

        return result;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long companyId) {
        Map<String, Object> companyCrudResult = new HashMap<>();
        companyCrudResult.put("status", "NOT_FOUND");
        companyCrudResult.put("operation", operation);
        companyCrudResult.put("message", "Company not found with ID: " + companyId);
        companyCrudResult.put("companyId", companyId);
        companyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("companyCrudResult", companyCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("companyId", companyId);
        result.put("companyName", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> companyCrudResult = new HashMap<>();
        companyCrudResult.put("status", "VALIDATION_ERROR");
        companyCrudResult.put("operation", operation);
        companyCrudResult.put("message", message);
        companyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("companyCrudResult", companyCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("companyId", null);
        result.put("companyName", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> companyCrudResult = new HashMap<>();
        companyCrudResult.put("status", "ERROR");
        companyCrudResult.put("operation", operation);
        companyCrudResult.put("message", "An unexpected error occurred while processing company operation");
        companyCrudResult.put("errorDetails", errorMessage);
        companyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("companyCrudResult", companyCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("companyId", null);
        result.put("companyName", null);

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

    private String extractString(Object value) {
        if (value == null) return null;
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
