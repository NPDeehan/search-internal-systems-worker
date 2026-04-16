package com.example.camunda.worker;

import com.example.camunda.exception.InsurancePolicyNotFoundException;
import com.example.camunda.model.InsurancePolicy;
import com.example.camunda.model.PolicyHolderType;
import com.example.camunda.model.PolicyStatus;
import com.example.camunda.model.PolicyType;
import com.example.camunda.service.InsurancePolicyService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InsurancePolicyCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(InsurancePolicyCrudWorker.class);

    private final InsurancePolicyService policyService;

    public InsurancePolicyCrudWorker(InsurancePolicyService policyService) {
        this.policyService = policyService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-insurance-policy job: {}", job.getKey());

        Map<String, Object> variables = job.getVariablesAsMap();
        String operation = normalizeOperation(variables.get("operation"));

        if (operation == null) {
            return buildValidationError("Operation must be one of CREATE, UPDATE, DELETE, QUERY", null);
        }

        try {
            return switch (operation) {
                case "CREATE" -> handleCreate(variables);
                case "UPDATE" -> handleUpdate(variables);
                case "DELETE" -> handleDelete(variables);
                case "QUERY"  -> handleQuery(variables);
                default -> buildValidationError("Unsupported operation: " + operation, operation);
            };
        } catch (Exception e) {
            log.error("Error processing insurance policy {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    // --- Operation handlers ---

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        PolicyType policyType       = extractPolicyType(variables.get("policyType"));
        PolicyHolderType holderType = extractHolderType(variables.get("holderType"));
        PolicyStatus status         = extractPolicyStatus(variables.get("status"));
        Long customerId             = extractLong(variables.get("customerId"));
        Long companyId              = extractLong(variables.get("companyId"));
        Long policyId               = extractLong(variables.get("policyId"));
        String policyNumber         = extractString(variables.get("policyNumber"));
        LocalDate startDate         = extractLocalDate(variables.get("startDate"));
        LocalDate endDate           = extractLocalDate(variables.get("endDate"));
        BigDecimal premiumAmount    = extractBigDecimal(variables.get("premiumAmount"));
        BigDecimal coverageAmount   = extractBigDecimal(variables.get("coverageAmount"));
        String notes                = extractString(variables.get("notes"));

        String validation = validateCreateInput(policyType, holderType, customerId, companyId, startDate, premiumAmount, coverageAmount);
        if (validation != null) {
            return buildValidationError(validation, "CREATE");
        }

        if (policyId != null && policyExists(policyId)) {
            return buildValidationError("Insurance policy already exists with ID: " + policyId, "CREATE");
        }

        InsurancePolicy policy = new InsurancePolicy();
        policy.setPolicyId(policyId);
        policy.setPolicyNumber(policyNumber);
        policy.setPolicyType(policyType);
        policy.setHolderType(holderType);
        policy.setStatus(status != null ? status : PolicyStatus.ACTIVE);
        policy.setCustomerId(holderType == PolicyHolderType.CUSTOMER ? customerId : null);
        policy.setCompanyId(holderType == PolicyHolderType.EXTERNAL_COMPANY ? companyId : null);
        policy.setStartDate(startDate);
        policy.setEndDate(endDate);
        policy.setPremiumAmount(premiumAmount);
        policy.setCoverageAmount(coverageAmount);
        policy.setNotes(notes);

        applyTypeSpecificFields(policy, policyType, variables);

        InsurancePolicy saved = policyService.savePolicy(policy);
        return buildSuccessResult("CREATE", saved, "Insurance policy created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long policyId = extractLong(variables.get("policyId"));
        if (policyId == null) {
            return buildValidationError("policyId is required for UPDATE operation", "UPDATE");
        }

        InsurancePolicy policy;
        try {
            policy = policyService.getPolicyById(policyId);
        } catch (InsurancePolicyNotFoundException ex) {
            return buildNotFoundResult("UPDATE", policyId);
        }

        PolicyType policyType       = extractPolicyType(variables.get("policyType"));
        PolicyHolderType holderType = extractHolderType(variables.get("holderType"));
        PolicyStatus status         = extractPolicyStatus(variables.get("status"));
        Long customerId             = extractLong(variables.get("customerId"));
        Long companyId              = extractLong(variables.get("companyId"));
        LocalDate startDate         = extractLocalDate(variables.get("startDate"));
        LocalDate endDate           = extractLocalDate(variables.get("endDate"));
        BigDecimal premiumAmount    = extractBigDecimal(variables.get("premiumAmount"));
        BigDecimal coverageAmount   = extractBigDecimal(variables.get("coverageAmount"));
        String notes                = extractString(variables.get("notes"));

        if (policyType  != null) policy.setPolicyType(policyType);
        if (holderType  != null) policy.setHolderType(holderType);
        if (status      != null) policy.setStatus(status);
        if (customerId  != null) policy.setCustomerId(customerId);
        if (companyId   != null) policy.setCompanyId(companyId);
        if (startDate   != null) policy.setStartDate(startDate);
        if (endDate     != null) policy.setEndDate(endDate);
        if (premiumAmount  != null) policy.setPremiumAmount(premiumAmount);
        if (coverageAmount != null) policy.setCoverageAmount(coverageAmount);
        if (notes       != null) policy.setNotes(notes);

        // Update type-specific fields based on the (potentially updated) policy type
        PolicyType effectiveType = policy.getPolicyType();
        applyTypeSpecificFields(policy, effectiveType, variables);

        InsurancePolicy saved = policyService.savePolicy(policy);
        return buildSuccessResult("UPDATE", saved, "Insurance policy updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long policyId = extractLong(variables.get("policyId"));
        if (policyId == null) {
            return buildValidationError("policyId is required for DELETE operation", "DELETE");
        }

        if (!policyExists(policyId)) {
            return buildNotFoundResult("DELETE", policyId);
        }

        policyService.deletePolicy(policyId);

        Map<String, Object> policyCrudResult = new HashMap<>();
        policyCrudResult.put("status", "SUCCESS");
        policyCrudResult.put("operation", "DELETE");
        policyCrudResult.put("message", "Insurance policy deleted successfully");
        policyCrudResult.put("policyId", policyId);
        policyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("policyCrudResult", policyCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("policyId", policyId);

        return result;
    }

    private Map<String, Object> handleQuery(Map<String, Object> variables) {
        Long policyId              = extractLong(variables.get("policyId"));
        Long customerId            = extractLong(variables.get("customerId"));
        Long companyId             = extractLong(variables.get("companyId"));
        PolicyType policyType      = extractPolicyType(variables.get("policyType"));
        PolicyStatus status        = extractPolicyStatus(variables.get("status"));
        PolicyHolderType holderType = extractHolderType(variables.get("holderType"));

        List<InsurancePolicy> policies;

        if (policyId != null) {
            // Single-record shortcut — policyId uniquely identifies a policy
            try {
                policies = List.of(policyService.getPolicyById(policyId));
            } catch (InsurancePolicyNotFoundException ex) {
                return buildNotFoundResult("QUERY", policyId);
            }
        } else {
            // Combined AND-filter across all provided criteria; empty criteria are ignored
            policies = policyService.searchPolicies(customerId, companyId, policyType, status, holderType);
        }

        List<Map<String, Object>> policyList = new ArrayList<>();
        for (InsurancePolicy p : policies) {
            policyList.add(buildPolicyData(p));
        }

        Map<String, Object> policyCrudResult = new HashMap<>();
        policyCrudResult.put("status", "SUCCESS");
        policyCrudResult.put("operation", "QUERY");
        policyCrudResult.put("message", "Query returned " + policyList.size() + " result(s)");
        policyCrudResult.put("policies", policyList);
        policyCrudResult.put("count", policyList.size());
        policyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("policyCrudResult", policyCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "QUERY");
        result.put("policyCount", policyList.size());

        return result;
    }

    // --- Type-specific field helpers ---

    private void applyTypeSpecificFields(InsurancePolicy policy, PolicyType type, Map<String, Object> variables) {
        if (type == null) return;
        switch (type) {
            case CAR -> {
                String reg   = extractString(variables.get("vehicleRegistration"));
                String make  = extractString(variables.get("vehicleMake"));
                String model = extractString(variables.get("vehicleModel"));
                Integer year = extractInteger(variables.get("vehicleYear"));
                if (reg   != null) policy.setVehicleRegistration(reg);
                if (make  != null) policy.setVehicleMake(make);
                if (model != null) policy.setVehicleModel(model);
                if (year  != null) policy.setVehicleYear(year);
            }
            case HOME -> {
                String addr     = extractString(variables.get("propertyAddress"));
                String propType = extractString(variables.get("propertyType"));
                BigDecimal val  = extractBigDecimal(variables.get("propertyValue"));
                if (addr     != null) policy.setPropertyAddress(addr);
                if (propType != null) policy.setPropertyType(propType);
                if (val      != null) policy.setPropertyValue(val);
            }
            case PET -> {
                String petName    = extractString(variables.get("petName"));
                String petSpecies = extractString(variables.get("petSpecies"));
                String petBreed   = extractString(variables.get("petBreed"));
                Integer petAge    = extractInteger(variables.get("petAge"));
                if (petName    != null) policy.setPetName(petName);
                if (petSpecies != null) policy.setPetSpecies(petSpecies);
                if (petBreed   != null) policy.setPetBreed(petBreed);
                if (petAge     != null) policy.setPetAge(petAge);
            }
            case LONG_TERM_INJURY -> {
                BigDecimal monthly = extractBigDecimal(variables.get("monthlyBenefit"));
                Integer waitDays   = extractInteger(variables.get("waitingPeriodDays"));
                Integer maxMonths  = extractInteger(variables.get("maxBenefitPeriodMonths"));
                if (monthly   != null) policy.setMonthlyBenefit(monthly);
                if (waitDays  != null) policy.setWaitingPeriodDays(waitDays);
                if (maxMonths != null) policy.setMaxBenefitPeriodMonths(maxMonths);
            }
        }
    }

    // --- Validation ---

    private String validateCreateInput(PolicyType policyType,
                                       PolicyHolderType holderType,
                                       Long customerId,
                                       Long companyId,
                                       LocalDate startDate,
                                       BigDecimal premiumAmount,
                                       BigDecimal coverageAmount) {
        if (policyType    == null) return "policyType is required for CREATE (LONG_TERM_INJURY, HOME, CAR, PET)";
        if (holderType    == null) return "holderType is required for CREATE (CUSTOMER, EXTERNAL_COMPANY)";
        if (holderType == PolicyHolderType.CUSTOMER && customerId == null)
            return "customerId is required when holderType is CUSTOMER";
        if (holderType == PolicyHolderType.EXTERNAL_COMPANY && companyId == null)
            return "companyId is required when holderType is EXTERNAL_COMPANY";
        if (startDate     == null) return "startDate is required for CREATE (YYYY-MM-DD)";
        if (premiumAmount == null) return "premiumAmount is required for CREATE";
        if (coverageAmount == null) return "coverageAmount is required for CREATE";
        return null;
    }

    private boolean policyExists(Long policyId) {
        try {
            policyService.getPolicyById(policyId);
            return true;
        } catch (InsurancePolicyNotFoundException ex) {
            return false;
        }
    }

    // --- Result builders ---

    private Map<String, Object> buildSuccessResult(String operation, InsurancePolicy policy, String message) {
        Map<String, Object> policyCrudResult = new HashMap<>();
        policyCrudResult.put("status", "SUCCESS");
        policyCrudResult.put("operation", operation);
        policyCrudResult.put("message", message);
        policyCrudResult.put("policy", buildPolicyData(policy));
        policyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("policyCrudResult", policyCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("policyId", policy.getPolicyId());
        result.put("policyNumber", policy.getPolicyNumber());

        return result;
    }

    private Map<String, Object> buildPolicyData(InsurancePolicy policy) {
        Map<String, Object> data = new HashMap<>();
        data.put("policyId",       policy.getPolicyId());
        data.put("policyNumber",   policy.getPolicyNumber());
        data.put("policyType",     policy.getPolicyType() != null ? policy.getPolicyType().name() : null);
        data.put("status",         policy.getStatus() != null ? policy.getStatus().name() : null);
        data.put("holderType",     policy.getHolderType() != null ? policy.getHolderType().name() : null);
        data.put("customerId",     policy.getCustomerId());
        data.put("companyId",      policy.getCompanyId());
        data.put("startDate",      policy.getStartDate() != null ? policy.getStartDate().toString() : null);
        data.put("endDate",        policy.getEndDate() != null ? policy.getEndDate().toString() : null);
        data.put("premiumAmount",  policy.getPremiumAmount());
        data.put("coverageAmount", policy.getCoverageAmount());
        data.put("notes",          policy.getNotes());
        // Type-specific
        data.put("vehicleRegistration", policy.getVehicleRegistration());
        data.put("vehicleMake",         policy.getVehicleMake());
        data.put("vehicleModel",        policy.getVehicleModel());
        data.put("vehicleYear",         policy.getVehicleYear());
        data.put("propertyAddress",     policy.getPropertyAddress());
        data.put("propertyType",        policy.getPropertyType());
        data.put("propertyValue",       policy.getPropertyValue());
        data.put("petName",             policy.getPetName());
        data.put("petSpecies",          policy.getPetSpecies());
        data.put("petBreed",            policy.getPetBreed());
        data.put("petAge",              policy.getPetAge());
        data.put("monthlyBenefit",      policy.getMonthlyBenefit());
        data.put("waitingPeriodDays",   policy.getWaitingPeriodDays());
        data.put("maxBenefitPeriodMonths", policy.getMaxBenefitPeriodMonths());
        return data;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long policyId) {
        Map<String, Object> policyCrudResult = new HashMap<>();
        policyCrudResult.put("status", "NOT_FOUND");
        policyCrudResult.put("operation", operation);
        policyCrudResult.put("message", "Insurance policy not found with ID: " + policyId);
        policyCrudResult.put("policyId", policyId);
        policyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("policyCrudResult", policyCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("policyId", policyId);
        result.put("policyNumber", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> policyCrudResult = new HashMap<>();
        policyCrudResult.put("status", "VALIDATION_ERROR");
        policyCrudResult.put("operation", operation);
        policyCrudResult.put("message", message);
        policyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("policyCrudResult", policyCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("policyId", null);
        result.put("policyNumber", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> policyCrudResult = new HashMap<>();
        policyCrudResult.put("status", "ERROR");
        policyCrudResult.put("operation", operation);
        policyCrudResult.put("message", "An unexpected error occurred while processing insurance policy operation");
        policyCrudResult.put("errorDetails", errorMessage);
        policyCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("policyCrudResult", policyCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("policyId", null);
        result.put("policyNumber", null);

        return result;
    }

    // --- Extraction helpers ---

    private String normalizeOperation(Object value) {
        String op = extractString(value);
        return op != null ? op.toUpperCase() : null;
    }

    private String extractString(Object value) {
        if (value == null) return null;
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try { return Long.parseLong(trimmed); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Integer extractInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try { return Integer.parseInt(trimmed); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private BigDecimal extractBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try { return new BigDecimal(trimmed); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private LocalDate extractLocalDate(Object value) {
        if (value == null) return null;
        String str = extractString(value);
        if (str == null) return null;
        try { return LocalDate.parse(str); } catch (DateTimeParseException e) { return null; }
    }

    private PolicyType extractPolicyType(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return PolicyType.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private PolicyStatus extractPolicyStatus(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return PolicyStatus.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private PolicyHolderType extractHolderType(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return PolicyHolderType.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }
}
