package com.example.camunda.dto;

import com.example.camunda.model.JobHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class JobHistoryDTO {
    private Long id;
    private String jobType;
    private String jobKey;
    private String status;
    private String executionTime;
    private String duration;
    private Map<String, Object> inputParameters;
    private Map<String, Object> results;
    private String errorMessage;
    private String workerName;
    private String processDefinition;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static JobHistoryDTO fromJobHistory(JobHistory jobHistory) {
        JobHistoryDTO dto = new JobHistoryDTO();
        dto.setId(jobHistory.getId());
        dto.setJobType(jobHistory.getJobType());
        dto.setJobKey(jobHistory.getJobKey());
        dto.setStatus(jobHistory.getStatus());
        dto.setExecutionTime(jobHistory.getExecutionTime().format(formatter));
        dto.setErrorMessage(jobHistory.getErrorMessage());
        
        // Calculate duration
        if (jobHistory.getExecutionTimeMs() != null) {
            long ms = jobHistory.getExecutionTimeMs();
            if (ms < 1000) {
                dto.setDuration(ms + " ms");
            } else if (ms < 60000) {
                dto.setDuration(String.format("%.1f s", ms / 1000.0));
            } else {
                dto.setDuration(String.format("%.1f min", ms / 60000.0));
            }
        } else {
            dto.setDuration("N/A");
        }
        
        // Set worker name based on job type
        dto.setWorkerName(getWorkerDisplayName(jobHistory.getJobType()));
        
        // Set process definition (simplified)
        dto.setProcessDefinition("Internal Systems Process");
        
        // Parse variables JSON to extract input parameters and results
        Map<String, Object> inputParams = new HashMap<>();
        Map<String, Object> results = new HashMap<>();
        
        if (jobHistory.getVariables() != null && !jobHistory.getVariables().trim().isEmpty()) {
            try {
                JsonNode variablesNode = objectMapper.readTree(jobHistory.getVariables());
                
                // Extract input parameters based on job type
                if ("match-customer-with-dri".equals(jobHistory.getJobType())) {
                    extractMatchCustomerInputs(variablesNode, inputParams);
                    extractMatchCustomerResults(variablesNode, results);
                } else if ("query-for-company".equals(jobHistory.getJobType())) {
                    extractCompanyQueryInputs(variablesNode, inputParams);
                    extractCompanyQueryResults(variablesNode, results);
                } else if ("search-employee".equals(jobHistory.getJobType())) {
                    extractEmployeeSearchInputs(variablesNode, inputParams);
                    extractEmployeeSearchResults(variablesNode, results);
                }
                
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse job variables JSON for job {}: {}", jobHistory.getId(), e.getMessage());
                inputParams.put("raw", jobHistory.getVariables());
            }
        }
        
        dto.setInputParameters(inputParams);
        dto.setResults(results);
        
        return dto;
    }
    
    private static String getWorkerDisplayName(String jobType) {
        switch (jobType) {
            case "match-customer-with-dri":
                return "Customer-DRI Matcher";
            case "query-for-company":
                return "Company Query Service";
            case "search-employee":
                return "Employee Search Service";
            default:
                return jobType;
        }
    }
    
    private static void extractMatchCustomerInputs(JsonNode variables, Map<String, Object> inputParams) {
        if (variables.has("customerId")) {
            inputParams.put("Customer ID", variables.get("customerId").asText());
        }
        if (variables.has("customerName")) {
            inputParams.put("Customer Name", variables.get("customerName").asText());
        }
    }
    
    private static void extractMatchCustomerResults(JsonNode variables, Map<String, Object> results) {
        if (variables.has("matchingResult")) {
            JsonNode matchingResult = variables.get("matchingResult");
            
            if (matchingResult.has("status")) {
                results.put("Status", matchingResult.get("status").asText());
            }
            
            if (matchingResult.has("customer")) {
                JsonNode customer = matchingResult.get("customer");
                if (customer.has("customerName")) {
                    results.put("Matched Customer", customer.get("customerName").asText());
                }
            }
            
            if (matchingResult.has("employee")) {
                JsonNode employee = matchingResult.get("employee");
                if (employee.has("fullName")) {
                    results.put("Assigned DRI", employee.get("fullName").asText());
                }
                if (employee.has("department")) {
                    results.put("Department", employee.get("department").asText());
                }
            }
        }
    }
    
    private static void extractCompanyQueryInputs(JsonNode variables, Map<String, Object> inputParams) {
        if (variables.has("companyName") && !variables.get("companyName").isNull()) {
            inputParams.put("Company Name", variables.get("companyName").asText());
        }
        if (variables.has("industry") && !variables.get("industry").isNull()) {
            inputParams.put("Industry", variables.get("industry").asText());
        }
        if (variables.has("city") && !variables.get("city").isNull()) {
            inputParams.put("City", variables.get("city").asText());
        }
        if (variables.has("revenue") && !variables.get("revenue").isNull()) {
            inputParams.put("Revenue", variables.get("revenue").asText());
        }
    }
    
    private static void extractCompanyQueryResults(JsonNode variables, Map<String, Object> results) {
        if (variables.has("companySearchResult")) {
            JsonNode searchResult = variables.get("companySearchResult");
            
            if (searchResult.has("status")) {
                results.put("Status", searchResult.get("status").asText());
            }
            
            if (searchResult.has("companies")) {
                JsonNode companies = searchResult.get("companies");
                if (companies.isArray()) {
                    results.put("Companies Found", companies.size());
                    
                    // Show first few company names
                    StringBuilder companyNames = new StringBuilder();
                    int count = 0;
                    for (JsonNode company : companies) {
                        if (count >= 3) {
                            companyNames.append("...");
                            break;
                        }
                        if (count > 0) companyNames.append(", ");
                        if (company.has("companyName")) {
                            companyNames.append(company.get("companyName").asText());
                        }
                        count++;
                    }
                    if (companyNames.length() > 0) {
                        results.put("Sample Companies", companyNames.toString());
                    }
                }
            }
        }
    }
    
    private static void extractEmployeeSearchInputs(JsonNode variables, Map<String, Object> inputParams) {
        if (variables.has("employeeName")) {
            inputParams.put("Employee Name", variables.get("employeeName").asText());
        }
        if (variables.has("department")) {
            inputParams.put("Department", variables.get("department").asText());
        }
        if (variables.has("jobTitle")) {
            inputParams.put("Job Title", variables.get("jobTitle").asText());
        }
        if (variables.has("exactMatch")) {
            inputParams.put("Exact Match", variables.get("exactMatch").asBoolean() ? "Yes" : "No");
        }
    }
    
    private static void extractEmployeeSearchResults(JsonNode variables, Map<String, Object> results) {
        if (variables.has("employeeSearchResult")) {
            JsonNode searchResult = variables.get("employeeSearchResult");
            
            if (searchResult.has("status")) {
                results.put("Search Status", searchResult.get("status").asText());
            }
            
            if (searchResult.has("employeeCount")) {
                results.put("Employees Found", searchResult.get("employeeCount").asInt());
            }
            
            if (searchResult.has("employees")) {
                JsonNode employees = searchResult.get("employees");
                if (employees.isArray() && employees.size() > 0) {
                    // Show first few employee names
                    StringBuilder employeeNames = new StringBuilder();
                    int count = 0;
                    for (JsonNode employee : employees) {
                        if (count >= 3) {
                            employeeNames.append("...");
                            break;
                        }
                        if (count > 0) employeeNames.append(", ");
                        if (employee.has("fullName")) {
                            employeeNames.append(employee.get("fullName").asText());
                            if (employee.has("department")) {
                                employeeNames.append(" (").append(employee.get("department").asText()).append(")");
                            }
                        }
                        count++;
                    }
                    if (employeeNames.length() > 0) {
                        results.put("Sample Employees", employeeNames.toString());
                    }
                }
            }
        }
        
        // Also check for individual employee fields (when single employee found)
        if (variables.has("employeeName") && variables.get("employeeName").isTextual()) {
            results.put("Employee Found", variables.get("employeeName").asText());
        }
        if (variables.has("employeeTitle") && variables.get("employeeTitle").isTextual()) {
            results.put("Job Title", variables.get("employeeTitle").asText());
        }
        if (variables.has("employeeDepartment") && variables.get("employeeDepartment").isTextual()) {
            results.put("Department", variables.get("employeeDepartment").asText());
        }
    }
}
