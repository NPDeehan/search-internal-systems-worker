package com.example.camunda.worker;

import com.example.camunda.model.ExternalCompany;
import com.example.camunda.service.CompanyService;
import com.example.camunda.service.JobHistoryService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryForCompanyWorkerTest {

    @Mock
    private CompanyService companyService;

    @Mock
    private JobHistoryService jobHistoryService;

    @Mock
    private JobClient jobClient;

    @Mock
    private ActivatedJob activatedJob;

    @InjectMocks
    private QueryForCompanyWorker worker;

    private ExternalCompany testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new ExternalCompany();
        testCompany.setCompanyId(1L);
        testCompany.setCompanyName("Test Company");
        testCompany.setAddress("123 Test St");
        testCompany.setContactPerson("Jane Smith");
        testCompany.setPhoneNumber("555-1234");
        testCompany.setCreatedAt(LocalDateTime.now());
        testCompany.setUpdatedAt(LocalDateTime.now());

        when(activatedJob.getKey()).thenReturn(67890L);
        when(activatedJob.getType()).thenReturn("query-for-company");
    }

    @Test
    void handleJob_WithValidCompanyId_ShouldCompleteSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("companyId", 1L);
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);
        when(companyService.getCompanyById(1L)).thenReturn(testCompany);

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(companyService).getCompanyById(1L);
        verify(jobClient).newCompleteCommand(67890L);
        verify(jobHistoryService).recordJobExecution(
                eq("query-for-company"),
                eq("67890"),
                eq("COMPLETED"),
                anyString(),
                isNull(),
                anyLong()
        );
    }

    @Test
    void handleJob_WithInvalidCompanyId_ShouldFailJob() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("companyId", 999L);
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);
        when(companyService.getCompanyById(999L))
                .thenThrow(new RuntimeException("Company not found"));

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(companyService).getCompanyById(999L);
        verify(jobClient).newFailCommand(67890L);
        verify(jobHistoryService).recordJobExecution(
                eq("query-for-company"),
                eq("67890"),
                eq("FAILED"),
                anyString(),
                eq("Company not found"),
                anyLong()
        );
    }

    @Test
    void handleJob_WithMissingCompanyId_ShouldFailJob() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("otherField", "value");
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(jobClient).newFailCommand(67890L);
        verify(jobHistoryService).recordJobExecution(
                eq("query-for-company"),
                eq("67890"),
                eq("FAILED"),
                anyString(),
                anyString(),
                anyLong()
        );
    }

    @Test
    void handleJob_WithInvalidCompanyIdType_ShouldFailJob() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("companyId", "invalid");
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(jobClient).newFailCommand(67890L);
        verify(jobHistoryService).recordJobExecution(
                eq("query-for-company"),
                eq("67890"),
                eq("FAILED"),
                anyString(),
                anyString(),
                anyLong()
        );
    }
}
