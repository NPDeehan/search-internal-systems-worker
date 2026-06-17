package com.example.camunda.worker;

import com.example.camunda.model.Customer;
import com.example.camunda.model.TrustLevel;
import com.example.camunda.service.CustomerService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QueryForCustomerWorker {

    private static final Logger log = LoggerFactory.getLogger(QueryForCustomerWorker.class);
    private final CustomerService customerService;

    public QueryForCustomerWorker(CustomerService customerService) {
        this.customerService = customerService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing query-for-customer job: {}", job.getKey());

        Map<String, Object> variables = job.getVariablesAsMap();
        Long customerId = extractLong(variables.get("customerId"));
        String customerName = extractString(variables.get("customerName"));
        String email = extractString(variables.get("email"));
        TrustLevel trustLevel = extractTrustLevel(variables.get("trustLevel"));
        Boolean fuzzyMatching = extractBoolean(variables.get("fuzzyMatching"));

        log.info("Querying customers - ID: {}, Name: '{}', Email: '{}', TrustLevel: {}, Fuzzy: {}",
                customerId, customerName, email, trustLevel, fuzzyMatching);

        if (customerId == null && isBlank(customerName) && isBlank(email) && trustLevel == null) {
            throw new IllegalArgumentException("At least one search parameter must be provided (customerId, customerName, email, or trustLevel)");
        }

        List<Customer> customers = customerService.searchCustomers(customerId, customerName, email, trustLevel, fuzzyMatching);

        Map<String, Object> searchParams = new HashMap<>();
        if (customerId != null) searchParams.put("customerId", customerId);
        if (!isBlank(customerName)) searchParams.put("customerName", customerName);
        if (!isBlank(email)) searchParams.put("email", email);
        if (trustLevel != null) searchParams.put("trustLevel", trustLevel.name());
        if (fuzzyMatching != null) searchParams.put("fuzzyMatching", fuzzyMatching);

        Map<String, Object> queryResult = new HashMap<>();
        queryResult.put("searchParameters", searchParams);
        queryResult.put("timestamp", java.time.LocalDateTime.now().toString());

        if (customers.isEmpty()) {
            log.info("No customers found for search criteria: {}", searchParams);
            queryResult.put("status", "NOT_FOUND");
            queryResult.put("message", "No customer records could be found with the provided search criteria");
            queryResult.put("customers", List.of());
        } else {
            List<Map<String, Object>> customerDataList = customers.stream()
                    .map(c -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("customerId", c.getCustomerId());
                        data.put("customerName", c.getCustomerName());
                        data.put("address", c.getAddress() != null ? c.getAddress() : "");
                        data.put("email", c.getEmail() != null ? c.getEmail() : "");
                        data.put("employeeId", c.getEmployeeId());
                        data.put("trustLevel", c.getTrustLevel() != null ? c.getTrustLevel().name() : "");
                        return data;
                    })
                    .collect(Collectors.toList());

            queryResult.put("status", "SUCCESS");
            queryResult.put("customers", customerDataList);
            log.info("Successfully found {} customer(s)", customers.size());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("customerSearchResult", queryResult);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private String extractString(Object value) {
        if (value == null) return null;
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private Boolean extractBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof String str) {
            if (str.trim().isEmpty()) return null;
            return "true".equalsIgnoreCase(str.trim()) || "1".equals(str.trim()) || "yes".equalsIgnoreCase(str.trim());
        }
        return null;
    }

    private TrustLevel extractTrustLevel(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return TrustLevel.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }
}
