package com.example.camunda;

import com.example.camunda.service.JobHistoryService;
import com.example.camunda.service.ZeebeConnectionService;
import com.example.camunda.worker.MatchCustomerWithDriWorker;
import com.example.camunda.worker.QueryForCompanyWorker;
import com.example.camunda.worker.EmployeeSearchWorker;
import com.example.camunda.worker.AccountSearchWorker;
import com.example.camunda.worker.AccountCrudWorker;
import com.example.camunda.worker.CompanyCrudWorker;
import com.example.camunda.worker.CustomerCrudWorker;
import com.example.camunda.worker.CustomerAccountLinkWorker;
import com.example.camunda.worker.EmployeeCrudWorker;
import com.example.camunda.worker.InsurancePolicyCrudWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ZeebeJobPollingService {
    
    private static final Logger log = LoggerFactory.getLogger(ZeebeJobPollingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final ZeebeConnectionService zeebeConnectionService;
    private final MatchCustomerWithDriWorker matchWorker;
    private final QueryForCompanyWorker companyWorker;
    private final EmployeeSearchWorker employeeSearchWorker;
    private final AccountSearchWorker accountSearchWorker;
    private final AccountCrudWorker accountCrudWorker;
    private final CustomerCrudWorker customerCrudWorker;
    private final EmployeeCrudWorker employeeCrudWorker;
    private final CompanyCrudWorker companyCrudWorker;
    private final CustomerAccountLinkWorker customerAccountLinkWorker;
    private final InsurancePolicyCrudWorker insurancePolicyCrudWorker;
    private final JobHistoryService jobHistoryService;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public ZeebeJobPollingService(ZeebeConnectionService zeebeConnectionService,
                                  MatchCustomerWithDriWorker matchWorker,
                                  QueryForCompanyWorker companyWorker,
                                  EmployeeSearchWorker employeeSearchWorker,
                                  AccountSearchWorker accountSearchWorker,
                                  AccountCrudWorker accountCrudWorker,
                                  CustomerCrudWorker customerCrudWorker,
                                  EmployeeCrudWorker employeeCrudWorker,
                                  CompanyCrudWorker companyCrudWorker,
                                  CustomerAccountLinkWorker customerAccountLinkWorker,
                                  InsurancePolicyCrudWorker insurancePolicyCrudWorker,
                                  JobHistoryService jobHistoryService) {
        this.zeebeConnectionService = zeebeConnectionService;
        this.matchWorker = matchWorker;
        this.companyWorker = companyWorker;
        this.employeeSearchWorker = employeeSearchWorker;
        this.accountSearchWorker = accountSearchWorker;
        this.accountCrudWorker = accountCrudWorker;
        this.customerCrudWorker = customerCrudWorker;
        this.employeeCrudWorker = employeeCrudWorker;
        this.companyCrudWorker = companyCrudWorker;
        this.customerAccountLinkWorker = customerAccountLinkWorker;
        this.insurancePolicyCrudWorker = insurancePolicyCrudWorker;
        this.jobHistoryService = jobHistoryService;
    }

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

    @Scheduled(fixedDelay = 1000) // Poll every second
    @Async
    public void pollAccountSearchJobs() {
        if (isRunning.get()) {
            pollJobs("search-account", accountSearchWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async
    public void pollAccountCrudJobs() {
        if (isRunning.get()) {
            pollJobs("manage-account-record", accountCrudWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async
    public void pollCustomerCrudJobs() {
        if (isRunning.get()) {
            pollJobs("manage-customer-record", customerCrudWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async
    public void pollEmployeeCrudJobs() {
        if (isRunning.get()) {
            pollJobs("manage-employee-record", employeeCrudWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async
    public void pollCompanyCrudJobs() {
        if (isRunning.get()) {
            pollJobs("manage-company-record", companyCrudWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async
    public void pollCustomerAccountLinkJobs() {
        if (isRunning.get()) {
            pollJobs("manage-customer-account-link", customerAccountLinkWorker::handleJob);
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Async
    public void pollInsurancePolicyCrudJobs() {
        if (isRunning.get()) {
            pollJobs("manage-insurance-policy", insurancePolicyCrudWorker::handleJob);
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
        String inputVariables = job.getVariables();

        try {
            log.debug("Processing job {} of type {}", jobKey, jobType);
            Object result = handler.apply(job);
            String outputVariables = toJson(result);
            
            zeebeConnectionService.getClient()
                    .newCompleteCommand(job.getKey())
                    .variables(result)
                    .send()
                    .join();

            long executionTime = System.currentTimeMillis() - startTime;
            jobHistoryService.recordJobSuccess(jobType, jobKey, inputVariables, outputVariables, executionTime);
            
            log.info("Completed job {} of type {} in {}ms", jobKey, jobType, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            zeebeConnectionService.getClient()
                    .newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();

            jobHistoryService.recordJobFailure(jobType, jobKey, inputVariables, e.getMessage(), executionTime);
            
            log.error("Failed job {} of type {} after {}ms: {}", jobKey, jobType, executionTime, e.getMessage());
        }
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize job output payload for history: {}", e.getMessage());
            return String.valueOf(payload);
        }
    }

    public boolean isPollingActive() {
        return isRunning.get();
    }
}
