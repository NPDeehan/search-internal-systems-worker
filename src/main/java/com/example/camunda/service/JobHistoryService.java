package com.example.camunda.service;

import com.example.camunda.model.JobHistory;
import com.example.camunda.repository.JobHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JobHistoryService {
    
    private final JobHistoryRepository jobHistoryRepository;

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
        return jobHistoryRepository.countJobsByType();
    }

    @Transactional
    public JobHistory recordJobExecution(String jobType, String jobKey, String status, 
                                       String variables, String errorMessage, long executionTimeMs) {
        log.debug("Recording job execution: type={}, key={}, status={}", jobType, jobKey, status);
        
        JobHistory jobHistory = new JobHistory();
        jobHistory.setJobType(jobType);
        jobHistory.setJobKey(jobKey);
        jobHistory.setStatus(status);
        jobHistory.setVariables(variables);
        jobHistory.setErrorMessage(errorMessage);
        jobHistory.setExecutionTime(LocalDateTime.now());
        jobHistory.setExecutionTimeMs(executionTimeMs);
        
        return jobHistoryRepository.save(jobHistory);
    }

    @Transactional
    public void recordJobSuccess(String jobType, String jobKey, String variables, long executionTimeMs) {
        recordJobExecution(jobType, jobKey, "COMPLETED", variables, null, executionTimeMs);
    }

    @Transactional
    public void recordJobFailure(String jobType, String jobKey, String variables, String errorMessage, long executionTimeMs) {
        recordJobExecution(jobType, jobKey, "FAILED", variables, errorMessage, executionTimeMs);
    }
}
