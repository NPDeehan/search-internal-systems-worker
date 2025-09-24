package com.example.camunda.worker;

import com.example.camunda.model.Employee;
import com.example.camunda.service.EmployeeService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeSearchWorker {

    private final EmployeeService employeeService;

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing search-employee job: {}", job.getKey());
        
        Map<String, Object> variables = job.getVariablesAsMap();
        log.debug("Raw variables received: {}", variables);
        
        String employeeName = extractString(variables.get("employeeName"));
        String department = extractString(variables.get("department"));
        String jobTitle = extractString(variables.get("jobTitle"));
        Boolean exactMatch = extractBoolean(variables.get("exactMatch"));
        Boolean fuzzyMatching = extractBoolean(variables.get("fuzzyMatching"));
        
        log.info("Searching employees - Name: '{}', Department: '{}', JobTitle: '{}', ExactMatch: {}, FuzzyMatching: {}", 
                employeeName, department, jobTitle, exactMatch, fuzzyMatching);
        
        // Validate that at least one search parameter is provided
        if (isEmpty(employeeName) && isEmpty(department) && isEmpty(jobTitle)) {
            String errorMsg = "At least one search parameter (employeeName, department, or jobTitle) must be provided";
            log.error("Validation failed: {}", errorMsg);
            
            Map<String, Object> result = new HashMap<>();
            result.put("employeeSearchResult", createValidationErrorResult(errorMsg, variables));
            result.put("searchStatus", "VALIDATION_ERROR");
            result.put("employees", List.of());
            result.put("employeeCount", 0);
            result.put("employeeId", null);
            result.put("employeeName", null);
            result.put("employeeTitle", null);
            result.put("employeeDepartment", null);
            result.put("employeePhone", null);
            
            return result;
        }

        try {
            List<Employee> employees;
            
            if (exactMatch != null && exactMatch && !isEmpty(employeeName)) {
                // Exact name match
                employees = employeeService.findEmployeeByName(employeeName)
                    .map(List::of)
                    .orElse(List.of());
            } else if (fuzzyMatching != null && fuzzyMatching) {
                // Fuzzy search using all provided parameters
                employees = employeeService.searchEmployeesFuzzy(employeeName, department, jobTitle);
            } else {
                // Flexible search using all provided parameters (existing behavior)
                employees = employeeService.searchEmployees(employeeName, department, jobTitle);
            }
            
            if (employees.isEmpty()) {
                log.info("No employees found with search criteria - Name: '{}', Department: '{}', JobTitle: '{}'", 
                        employeeName, department, jobTitle);
                
                Map<String, Object> result = new HashMap<>();
                result.put("employeeSearchResult", createNotFoundResult(
                    "No employee records could be found with the provided search criteria", variables));
                result.put("searchStatus", "NOT_FOUND");
                result.put("employees", List.of());
                result.put("employeeCount", 0);
                result.put("employeeId", null);
                result.put("employeeName", null);
                result.put("employeeTitle", null);
                result.put("employeeDepartment", null);
                result.put("employeePhone", null);
                
                return result;
            } else {
                log.info("Found {} employee(s) matching the search criteria", employees.size());
                
                Map<String, Object> result = new HashMap<>();
                result.put("employeeSearchResult", createSuccessResult(employees, variables));
                result.put("searchStatus", "SUCCESS");
                result.put("employees", convertEmployeesToMaps(employees));
                result.put("employeeCount", employees.size());
                
                // If only one employee found, add individual employee fields for easy access
                if (employees.size() == 1) {
                    Employee employee = employees.get(0);
                    result.put("employeeId", employee.getEmployeeId());
                    result.put("employeeName", employee.getFullName());
                    result.put("employeeTitle", employee.getJobTitle());
                    result.put("employeeDepartment", employee.getDepartment());
                    result.put("employeePhone", employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "");
                } else {
                    // Multiple employees found - clear individual fields
                    result.put("employeeId", null);
                    result.put("employeeName", null);
                    result.put("employeeTitle", null);
                    result.put("employeeDepartment", null);
                    result.put("employeePhone", null);
                }
                
                return result;
            }
        } catch (Exception e) {
            log.error("Error occurred while searching for employees: {}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("employeeSearchResult", createErrorResult(e.getMessage(), variables));
            result.put("searchStatus", "ERROR");
            result.put("employees", List.of());
            result.put("employeeCount", 0);
            result.put("employeeId", null);
            result.put("employeeName", null);
            result.put("employeeTitle", null);
            result.put("employeeDepartment", null);
            result.put("employeePhone", null);
            
            return result;
        }
    }
    
    private Map<String, Object> createSuccessResult(List<Employee> employees, Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("employeeCount", employees.size());
        result.put("employees", convertEmployeesToMaps(employees));
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        result.put("searchParameters", extractSearchParameters(searchParams));
        return result;
    }
    
    private Map<String, Object> createNotFoundResult(String message, Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "NOT_FOUND");
        result.put("message", message);
        result.put("employeeCount", 0);
        result.put("employees", List.of());
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        result.put("searchParameters", extractSearchParameters(searchParams));
        return result;
    }
    
    private Map<String, Object> createValidationErrorResult(String message, Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "VALIDATION_ERROR");
        result.put("message", message);
        result.put("employeeCount", 0);
        result.put("employees", List.of());
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        result.put("searchParameters", extractSearchParameters(searchParams));
        return result;
    }
    
    private Map<String, Object> createErrorResult(String errorMessage, Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ERROR");
        result.put("message", "An error occurred during employee search");
        result.put("errorDetails", errorMessage);
        result.put("employeeCount", 0);
        result.put("employees", List.of());
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        result.put("searchParameters", extractSearchParameters(searchParams));
        return result;
    }
    
    private List<Map<String, Object>> convertEmployeesToMaps(List<Employee> employees) {
        return employees.stream()
            .map(employee -> {
                Map<String, Object> empData = new HashMap<>();
                empData.put("employeeId", employee.getEmployeeId());
                empData.put("fullName", employee.getFullName());
                empData.put("jobTitle", employee.getJobTitle());
                empData.put("department", employee.getDepartment());
                empData.put("phoneNumber", employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "");
                return empData;
            })
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> extractSearchParameters(Map<String, Object> variables) {
        Map<String, Object> searchParams = new HashMap<>();
        
        String employeeName = extractString(variables.get("employeeName"));
        String department = extractString(variables.get("department"));
        String jobTitle = extractString(variables.get("jobTitle"));
        Boolean exactMatch = extractBoolean(variables.get("exactMatch"));
        Boolean fuzzyMatching = extractBoolean(variables.get("fuzzyMatching"));
        
        if (!isEmpty(employeeName)) searchParams.put("employeeName", employeeName);
        if (!isEmpty(department)) searchParams.put("department", department);
        if (!isEmpty(jobTitle)) searchParams.put("jobTitle", jobTitle);
        if (exactMatch != null) searchParams.put("exactMatch", exactMatch);
        if (fuzzyMatching != null) searchParams.put("fuzzyMatching", fuzzyMatching);
        
        return searchParams;
    }
    
    private String extractString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String str = (String) value;
            return str.trim().isEmpty() ? null : str.trim();
        }
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    private Boolean extractBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = (String) value;
            return Boolean.parseBoolean(str.trim());
        }
        return null;
    }
    
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
