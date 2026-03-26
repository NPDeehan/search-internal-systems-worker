package com.example.camunda.service;

import com.example.camunda.model.JobHistory;
import com.example.camunda.repository.JobHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class JobHistoryService {
    
    private static final Logger log = LoggerFactory.getLogger(JobHistoryService.class);
    
    private final JobHistoryRepository jobHistoryRepository;

    public JobHistoryService(JobHistoryRepository jobHistoryRepository) {
        this.jobHistoryRepository = jobHistoryRepository;
    }

    public List<JobHistory> getRecentJobHistory(int limit) {
        log.debug("Fetching recent job history, limit: {}", limit);
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("executionTime").descending());
        return jobHistoryRepository.findAll(pageRequest).getContent();
    }

    public List<JobHistory> getAllJobHistory() {
        return jobHistoryRepository.findAll(Sort.by("executionTime").descending());
    }

    public long getTotalJobsProcessed() {
        return jobHistoryRepository.count();
    }

    public long getJobsProcessedToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return jobHistoryRepository.countByExecutionTimeAfter(startOfDay);
    }

    public Map<String, Long> getJobCountsByType() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : jobHistoryRepository.countJobsByTypeRaw()) {
            if (row == null || row.length < 2) {
                continue;
            }
            String jobType = row[0] == null ? "UNKNOWN" : row[0].toString();
            Long count = row[1] instanceof Number number ? number.longValue() : 0L;
            counts.put(jobType, count);
        }
        return counts;
    }

    @Transactional
    public JobHistory recordJobExecution(String jobType, String jobKey, String status, 
                                       String inputVariables,
                                       String outputVariables,
                                       String errorMessage,
                                       long executionTimeMs) {
        log.debug("Recording job execution: type={}, key={}, status={}", jobType, jobKey, status);
        
        JobHistory jobHistory = new JobHistory();
        jobHistory.setJobType(jobType);
        jobHistory.setJobKey(jobKey);
        jobHistory.setStatus(status);
        jobHistory.setInputVariables(inputVariables);
        jobHistory.setOutputVariables(outputVariables);
        jobHistory.setErrorMessage(errorMessage);
        jobHistory.setExecutionTime(LocalDateTime.now());
        jobHistory.setExecutionTimeMs(executionTimeMs);
        
        return jobHistoryRepository.save(jobHistory);
    }

    @Transactional
    public void recordJobSuccess(String jobType,
                                 String jobKey,
                                 String inputVariables,
                                 String outputVariables,
                                 long executionTimeMs) {
        recordJobExecution(jobType, jobKey, "COMPLETED", inputVariables, outputVariables, null, executionTimeMs);
    }

    @Transactional
    public void recordJobFailure(String jobType,
                                 String jobKey,
                                 String inputVariables,
                                 String errorMessage,
                                 long executionTimeMs) {
        recordJobExecution(jobType, jobKey, "FAILED", inputVariables, null, errorMessage, executionTimeMs);
    }
}
