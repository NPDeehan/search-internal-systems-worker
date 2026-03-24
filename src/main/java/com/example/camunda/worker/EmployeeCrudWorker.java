package com.example.camunda.worker;

import com.example.camunda.model.Employee;
import com.example.camunda.service.EmployeeService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmployeeCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCrudWorker.class);

    private final EmployeeService employeeService;

    public EmployeeCrudWorker(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-employee-record job: {}", job.getKey());

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
            log.error("Error processing employee {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long employeeId = extractLong(variables.get("employeeId"));
        String fullName = extractString(variables.get("fullName"));
        String jobTitle = extractString(variables.get("jobTitle"));
        String department = extractString(variables.get("department"));
        String phoneNumber = extractString(variables.get("phoneNumber"));

        String validationMessage = validateCreateInput(fullName, jobTitle, department);
        if (validationMessage != null) {
            return buildValidationError(validationMessage, "CREATE");
        }

        if (employeeId != null && employeeService.findEmployeeById(employeeId).isPresent()) {
            return buildValidationError("Employee already exists with ID: " + employeeId, "CREATE");
        }

        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setFullName(fullName);
        employee.setJobTitle(jobTitle);
        employee.setDepartment(department);
        employee.setPhoneNumber(phoneNumber);

        Employee saved = employeeService.saveEmployee(employee);
        return buildSuccessResult("CREATE", saved, "Employee created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long employeeId = extractLong(variables.get("employeeId"));
        if (employeeId == null) {
            return buildValidationError("employeeId is required for UPDATE operation", "UPDATE");
        }

        if (employeeService.findEmployeeById(employeeId).isEmpty()) {
            return buildNotFoundResult("UPDATE", employeeId);
        }

        Employee employee = employeeService.getEmployeeById(employeeId);

        String fullName = extractString(variables.get("fullName"));
        String jobTitle = extractString(variables.get("jobTitle"));
        String department = extractString(variables.get("department"));
        String phoneNumber = extractString(variables.get("phoneNumber"));

        if (fullName != null) employee.setFullName(fullName);
        if (jobTitle != null) employee.setJobTitle(jobTitle);
        if (department != null) employee.setDepartment(department);
        if (phoneNumber != null) employee.setPhoneNumber(phoneNumber);

        Employee saved = employeeService.saveEmployee(employee);
        return buildSuccessResult("UPDATE", saved, "Employee updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long employeeId = extractLong(variables.get("employeeId"));
        if (employeeId == null) {
            return buildValidationError("employeeId is required for DELETE operation", "DELETE");
        }

        if (employeeService.findEmployeeById(employeeId).isEmpty()) {
            return buildNotFoundResult("DELETE", employeeId);
        }

        employeeService.deleteEmployee(employeeId);

        Map<String, Object> employeeCrudResult = new HashMap<>();
        employeeCrudResult.put("status", "SUCCESS");
        employeeCrudResult.put("operation", "DELETE");
        employeeCrudResult.put("message", "Employee deleted successfully");
        employeeCrudResult.put("employeeId", employeeId);
        employeeCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("employeeCrudResult", employeeCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("employeeId", employeeId);
        result.put("employeeName", null);

        return result;
    }

    private String validateCreateInput(String fullName,
                                       String jobTitle,
                                       String department) {
        if (fullName == null) return "fullName is required for CREATE operation";
        if (jobTitle == null) return "jobTitle is required for CREATE operation";
        if (department == null) return "department is required for CREATE operation";
        return null;
    }

    private Map<String, Object> buildSuccessResult(String operation, Employee employee, String message) {
        Map<String, Object> employeeData = new HashMap<>();
        employeeData.put("employeeId", employee.getEmployeeId());
        employeeData.put("fullName", employee.getFullName());
        employeeData.put("jobTitle", employee.getJobTitle());
        employeeData.put("department", employee.getDepartment());
        employeeData.put("phoneNumber", employee.getPhoneNumber());

        Map<String, Object> employeeCrudResult = new HashMap<>();
        employeeCrudResult.put("status", "SUCCESS");
        employeeCrudResult.put("operation", operation);
        employeeCrudResult.put("message", message);
        employeeCrudResult.put("employee", employeeData);
        employeeCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("employeeCrudResult", employeeCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("employeeId", employee.getEmployeeId());
        result.put("employeeName", employee.getFullName());

        return result;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long employeeId) {
        Map<String, Object> employeeCrudResult = new HashMap<>();
        employeeCrudResult.put("status", "NOT_FOUND");
        employeeCrudResult.put("operation", operation);
        employeeCrudResult.put("message", "Employee not found with ID: " + employeeId);
        employeeCrudResult.put("employeeId", employeeId);
        employeeCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("employeeCrudResult", employeeCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("employeeId", employeeId);
        result.put("employeeName", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> employeeCrudResult = new HashMap<>();
        employeeCrudResult.put("status", "VALIDATION_ERROR");
        employeeCrudResult.put("operation", operation);
        employeeCrudResult.put("message", message);
        employeeCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("employeeCrudResult", employeeCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("employeeId", null);
        result.put("employeeName", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> employeeCrudResult = new HashMap<>();
        employeeCrudResult.put("status", "ERROR");
        employeeCrudResult.put("operation", operation);
        employeeCrudResult.put("message", "An unexpected error occurred while processing employee operation");
        employeeCrudResult.put("errorDetails", errorMessage);
        employeeCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("employeeCrudResult", employeeCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("employeeId", null);
        result.put("employeeName", null);

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
