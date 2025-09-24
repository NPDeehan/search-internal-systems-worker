package com.example.camunda.worker;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.service.CustomerService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchCustomerWithDriWorker {
    
    private final CustomerService customerService;

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing match-customer-with-dri job: {}", job.getKey());
        
        Map<String, Object> variables = job.getVariablesAsMap();
        log.debug("Raw variables received: {}", variables);
        
        Long customerId = extractLong(variables.get("customerId"));
        String customerName = extractString(variables.get("customerName"));
        Boolean allowMultiple = extractBoolean(variables.get("allowMultiple"));
        Boolean fuzzyMatching = extractBoolean(variables.get("fuzzyMatching"));
        
        log.info("Matching customer - ID: {}, Name: '{}', Allow Multiple: {}, Fuzzy Matching: {}", 
                customerId, customerName, allowMultiple, fuzzyMatching);
        
        // Validate that at least one parameter is provided
        if (customerId == null && (customerName == null || customerName.trim().isEmpty())) {
            String errorMsg = "At least one search parameter (customerId or customerName) must be provided";
            log.error("Validation failed: {}", errorMsg);
            
            Map<String, Object> matchingResult = new HashMap<>();
            matchingResult.put("status", "VALIDATION_ERROR");
            matchingResult.put("error", errorMsg);
            matchingResult.put("timestamp", java.time.LocalDateTime.now().toString());
            
            Map<String, Object> result = new HashMap<>();
            result.put("matchingResult", matchingResult);
            result.put("matchStatus", "VALIDATION_ERROR");
            result.put("customers", List.of());
            result.put("customerCount", 0);
            result.put("customerId", null);
            result.put("customerName", null);
            result.put("employeeId", null);
            result.put("employeeName", null);
            result.put("employeeTitle", null);
            result.put("employeeDepartment", null);
            result.put("employeePhone", null);
            
            return result;
        }
        
        // Log what we're actually searching for
        log.info("Searching for customer with - customerId: {}, customerName: '{}'", customerId, customerName);
        
        try {
            List<Customer> customers;
            
            if (allowMultiple != null && allowMultiple) {
                // Allow multiple customer matches
                customers = customerService.getCustomersWithEmployees(customerId, customerName, fuzzyMatching);
            } else {
                // Single customer match (backward compatibility)
                try {
                    Customer customer = customerService.getCustomerWithEmployee(customerId, customerName, fuzzyMatching);
                    customers = List.of(customer);
                } catch (Exception e) {
                    customers = List.of();
                }
            }
            
            if (customers.isEmpty()) {
                log.info("No customer record found with parameters - ID: {}, Name: '{}'. Returning 'not found' response.", 
                         customerId, customerName);
                
                // Create "not found" response instead of throwing error
                Map<String, Object> matchingResult = new HashMap<>();
                matchingResult.put("status", "NOT_FOUND");
                matchingResult.put("message", "No customer record could be found with the provided search criteria");
                matchingResult.put("timestamp", java.time.LocalDateTime.now().toString());
                
                // Include search parameters used
                Map<String, Object> searchParams = new HashMap<>();
                if (customerId != null) searchParams.put("customerId", customerId);
                if (customerName != null && !customerName.trim().isEmpty()) searchParams.put("customerName", customerName);
                if (fuzzyMatching != null) searchParams.put("fuzzyMatching", fuzzyMatching);
                matchingResult.put("searchParameters", searchParams);
                
                Map<String, Object> result = new HashMap<>();
                result.put("matchingResult", matchingResult);
                result.put("matchStatus", "NOT_FOUND");
                result.put("customers", List.of());
                result.put("customerCount", 0);
                result.put("customerId", null);
                result.put("customerName", null);
                result.put("employeeId", null);
                result.put("employeeName", null);
                result.put("employeeTitle", null);
                result.put("employeeDepartment", null);
                result.put("employeePhone", null);
                
                return result;
            }
            
            // Process all found customers and their employees
            List<Map<String, Object>> customerEmployeePairs = new ArrayList<>();
            
            for (Customer customer : customers) {
                Employee employee = customerService.getEmployeeForCustomer(customer);
                
                Map<String, Object> customerData = new HashMap<>();
                customerData.put("customerId", customer.getCustomerId());
                customerData.put("customerName", customer.getCustomerName());
                customerData.put("employeeId", customer.getEmployeeId());
                
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("employeeId", employee.getEmployeeId());
                employeeData.put("fullName", employee.getFullName());
                employeeData.put("jobTitle", employee.getJobTitle());
                employeeData.put("department", employee.getDepartment());
                employeeData.put("phoneNumber", employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "");
                
                Map<String, Object> pair = new HashMap<>();
                pair.put("customer", customerData);
                pair.put("employee", employeeData);
                
                customerEmployeePairs.add(pair);
            }
            
            // Create consolidated result object
            Map<String, Object> matchingResult = new HashMap<>();
            matchingResult.put("status", "SUCCESS");
            matchingResult.put("customers", customerEmployeePairs);
            matchingResult.put("customerCount", customers.size());
            matchingResult.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // Include search parameters used
            Map<String, Object> searchParams = new HashMap<>();
            if (customerId != null) searchParams.put("customerId", customerId);
            if (customerName != null && !customerName.trim().isEmpty()) searchParams.put("customerName", customerName);
            if (allowMultiple != null) searchParams.put("allowMultiple", allowMultiple);
            if (fuzzyMatching != null) searchParams.put("fuzzyMatching", fuzzyMatching);
            matchingResult.put("searchParameters", searchParams);
            
            Map<String, Object> result = new HashMap<>();
            result.put("matchingResult", matchingResult);
            result.put("matchStatus", "SUCCESS");
            result.put("customers", customerEmployeePairs);
            result.put("customerCount", customers.size());
            
            // If only one customer found, add individual fields for easy access (backward compatibility)
            if (customers.size() == 1) {
                Customer customer = customers.get(0);
                Employee employee = customerService.getEmployeeForCustomer(customer);
                
                result.put("customerId", customer.getCustomerId());
                result.put("customerName", customer.getCustomerName());
                result.put("employeeId", employee.getEmployeeId());
                result.put("employeeName", employee.getFullName());
                result.put("employeeTitle", employee.getJobTitle());
                result.put("employeeDepartment", employee.getDepartment());
                result.put("employeePhone", employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "");
            } else {
                // Multiple customers found - clear individual fields
                result.put("customerId", null);
                result.put("customerName", null);
                result.put("employeeId", null);
                result.put("employeeName", null);
                result.put("employeeTitle", null);
                result.put("employeeDepartment", null);
                result.put("employeePhone", null);
            }
            
            log.info("Successfully matched {} customer(s) with their DRI employees", customers.size());
            
            return result;
        } catch (Exception e) {
            log.error("Error occurred while matching customer with DRI: {}", e.getMessage(), e);
            
            Map<String, Object> matchingResult = new HashMap<>();
            matchingResult.put("status", "ERROR");
            matchingResult.put("error", e.getMessage());
            matchingResult.put("timestamp", java.time.LocalDateTime.now().toString());
            
            Map<String, Object> result = new HashMap<>();
            result.put("matchingResult", matchingResult);
            result.put("matchStatus", "ERROR");
            result.put("customers", List.of());
            result.put("customerCount", 0);
            result.put("customerId", null);
            result.put("customerName", null);
            result.put("employeeId", null);
            result.put("employeeName", null);
            result.put("employeeTitle", null);
            result.put("employeeDepartment", null);
            result.put("employeePhone", null);
            
            return result;
        }
    }
    
    private Long extractLong(Object value) {
        if (value == null) {
            log.trace("extractLong: value is null");
            return null;
        }
        if (value instanceof Number) {
            Long result = ((Number) value).longValue();
            log.trace("extractLong: converted Number {} to Long {}", value, result);
            return result;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.trim().isEmpty()) {
                log.trace("extractLong: string is empty");
                return null;
            }
            try {
                Long result = Long.parseLong(str.trim());
                log.trace("extractLong: converted String '{}' to Long {}", str, result);
                return result;
            } catch (NumberFormatException e) {
                log.warn("extractLong: failed to parse '{}' as Long: {}", str, e.getMessage());
                return null;
            }
        }
        log.warn("extractLong: unexpected value type {}: {}", value.getClass().getSimpleName(), value);
        return null;
    }
    
    private String extractString(Object value) {
        if (value == null) {
            log.trace("extractString: value is null");
            return null;
        }
        if (value instanceof String) {
            String str = (String) value;
            String result = str.trim().isEmpty() ? null : str.trim();
            log.trace("extractString: converted String '{}' to '{}'", str, result);
            return result;
        }
        String result = value.toString().trim();
        log.trace("extractString: converted {} to String '{}'", value.getClass().getSimpleName(), result);
        return result.isEmpty() ? null : result;
    }
    
    private Boolean extractBoolean(Object value) {
        if (value == null) {
            log.trace("extractBoolean: null value, returning null");
            return null;
        }
        if (value instanceof Boolean) {
            Boolean result = (Boolean) value;
            log.trace("extractBoolean: Boolean value '{}' used directly", result);
            return result;
        }
        
        String str = value.toString().trim().toLowerCase();
        if (str.isEmpty()) {
            log.trace("extractBoolean: empty string, returning null");
            return null;
        }
        
        Boolean result = "true".equals(str) || "1".equals(str) || "yes".equals(str);
        log.trace("extractBoolean: converted '{}' to Boolean '{}'", str, result);
        return result;
    }
}
