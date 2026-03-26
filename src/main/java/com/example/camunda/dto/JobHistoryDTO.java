package com.example.camunda.dto;

import com.example.camunda.model.JobHistory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class JobHistoryDTO {
    private static final Logger log = LoggerFactory.getLogger(JobHistoryDTO.class);
    
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
    private String rawInputVariables;
    private String rawOutputVariables;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Map<String, Object> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(Map<String, Object> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public Map<String, Object> getResults() {
        return results;
    }

    public void setResults(Map<String, Object> results) {
        this.results = results;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(String processDefinition) {
        this.processDefinition = processDefinition;
    }

    public String getRawInputVariables() {
        return rawInputVariables;
    }

    public void setRawInputVariables(String rawInputVariables) {
        this.rawInputVariables = rawInputVariables;
    }

    public String getRawOutputVariables() {
        return rawOutputVariables;
    }

    public void setRawOutputVariables(String rawOutputVariables) {
        this.rawOutputVariables = rawOutputVariables;
    }

    public static JobHistoryDTO fromJobHistory(JobHistory jobHistory) {
        JobHistoryDTO dto = new JobHistoryDTO();
        dto.setId(jobHistory.getId());
        dto.setJobType(jobHistory.getJobType());
        dto.setJobKey(jobHistory.getJobKey());
        dto.setStatus(jobHistory.getStatus());
        dto.setExecutionTime(jobHistory.getExecutionTime().format(formatter));
        dto.setErrorMessage(jobHistory.getErrorMessage());
        dto.setRawInputVariables(jobHistory.getInputVariables());
        dto.setRawOutputVariables(jobHistory.getOutputVariables());
        
        // Calculate duration
        if (jobHistory.getExecutionTimeMs() != null) {
            long ms = jobHistory.getExecutionTimeMs();
            if (ms < 1000) {
                dto.setDuration(ms + " ms");
            } else if (ms < 60000) {
                dto.setDuration("%.1f s".formatted(ms / 1000.0));
            } else {
                dto.setDuration("%.1f min".formatted(ms / 60000.0));
            }
        } else {
            dto.setDuration("N/A");
        }
        
        // Set worker name based on job type
        dto.setWorkerName(getWorkerDisplayName(jobHistory.getJobType()));
        
        // Set process definition (placeholder)
        dto.setProcessDefinition("Internal Systems Process");
        dto.setInputParameters(parseJsonToMap(jobHistory.getInputVariables(), "input", jobHistory.getId()));
        dto.setResults(parseJsonToMap(jobHistory.getOutputVariables(), "output", jobHistory.getId()));
        
        return dto;
    }

    private static Map<String, Object> parseJsonToMap(String json, String label, Long jobId) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse {} JSON for job {}: {}", label, jobId, e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("_raw", json);
            return fallback;
        }
    }
    
    private static String getWorkerDisplayName(String jobType) {
        switch (jobType) {
            case "match-customer-with-dri":
                return "Customer-DRI Matcher";
            case "query-for-company":
                return "Company Query Service";
            case "search-employee":
                return "Employee Search Service";
            case "search-account":
                return "Account Search Service";
            case "manage-account-record":
                return "Account CRUD Service";
            case "manage-customer-record":
                return "Customer CRUD Service";
            case "manage-employee-record":
                return "Employee CRUD Service";
            case "manage-company-record":
                return "Company CRUD Service";
            case "manage-customer-account-link":
                return "Customer-Account Link Service";
            default:
                return jobType;
        }
    }
}
