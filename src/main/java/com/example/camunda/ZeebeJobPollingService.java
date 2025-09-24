package com.example.camunda;

import com.example.camunda.service.JobHistoryService;
import com.example.camunda.service.ZeebeConnectionService;
import com.example.camunda.worker.MatchCustomerWithDriWorker;
import com.example.camunda.worker.QueryForCompanyWorker;
import com.example.camunda.worker.EmployeeSearchWorker;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZeebeJobPollingService {
    
    private final ZeebeConnectionService zeebeConnectionService;
    private final MatchCustomerWithDriWorker matchWorker;
    private final QueryForCompanyWorker companyWorker;
    private final EmployeeSearchWorker employeeSearchWorker;
    private final JobHistoryService jobHistoryService;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @PostConstruct
    public void startPolling() {
        isRunning.set(true);
        log.info("Starting Zeebe job polling service");
    }

    @PreDestroy
    public void stopPolling() {
        isRunning.set(false);
        log.info("Stopping Zeebe job polling service");
    }

    @Scheduled(fixedDelay = 1000) // Poll every second
    @Async
    public void pollMatchCustomerJobs() {
        if (isRunning.get()) {
            pollJobs("match-customer-with-dri", matchWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000) // Poll every second
    @Async
    public void pollCompanyJobs() {
        if (isRunning.get()) {
            pollJobs("query-for-company", companyWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000) // Poll every second
    @Async
    public void pollEmployeeSearchJobs() {
        if (isRunning.get()) {
            pollJobs("search-employee", employeeSearchWorker::handleJob);
        }
    }

    private void pollJobs(String jobType, java.util.function.Function<ActivatedJob, Object> handler) {
        if (!zeebeConnectionService.isConnected()) {
            log.debug("Zeebe not connected, skipping poll for job type: {}", jobType);
            return;
        }

        try {
            List<ActivatedJob> jobs = zeebeConnectionService.getClient()
                    .newActivateJobsCommand()
                    .jobType(jobType)
                    .maxJobsToActivate(5)
                    .timeout(Duration.ofMinutes(1))
                    .send()
                    .join()
                    .getJobs();

            for (ActivatedJob job : jobs) {
                processJob(job, jobType, handler);
            }

        } catch (Exception e) {
            log.error("Polling error for job type {}: {}", jobType, e.getMessage());
        }
    }

    private void processJob(ActivatedJob job, String jobType, java.util.function.Function<ActivatedJob, Object> handler) {
        long startTime = System.currentTimeMillis();
        String jobKey = String.valueOf(job.getKey());
        String variables = job.getVariables();

        try {
            log.debug("Processing job {} of type {}", jobKey, jobType);
            Object result = handler.apply(job);
            
            zeebeConnectionService.getClient()
                    .newCompleteCommand(job.getKey())
                    .variables(result)
                    .send()
                    .join();

            long executionTime = System.currentTimeMillis() - startTime;
            jobHistoryService.recordJobSuccess(jobType, jobKey, variables, executionTime);
            
            log.info("Completed job {} of type {} in {}ms", jobKey, jobType, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            zeebeConnectionService.getClient()
                    .newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();

            jobHistoryService.recordJobFailure(jobType, jobKey, variables, e.getMessage(), executionTime);
            
            log.error("Failed job {} of type {} after {}ms: {}", jobKey, jobType, executionTime, e.getMessage());
        }
    }

    public boolean isPollingActive() {
        return isRunning.get();
    }
}
