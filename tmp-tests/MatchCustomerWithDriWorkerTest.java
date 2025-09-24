package com.example.camunda.worker;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.service.CustomerService;
import com.example.camunda.service.EmployeeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchCustomerWithDriWorkerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private JobHistoryService jobHistoryService;

    @Mock
    private JobClient jobClient;

    @Mock
    private ActivatedJob activatedJob;

    @InjectMocks
    private MatchCustomerWithDriWorker worker;

    private Customer testCustomer;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1L);
        testEmployee.setFullName("John Doe");
        testEmployee.setJobTitle("Sales Manager");
        testEmployee.setDepartment("Sales");
        testEmployee.setCreatedAt(LocalDateTime.now());
        testEmployee.setUpdatedAt(LocalDateTime.now());

        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setCustomerName("Test Customer");
        testCustomer.setEmployeeId(1L);
        testCustomer.setCreatedAt(LocalDateTime.now());
        testCustomer.setUpdatedAt(LocalDateTime.now());

        when(activatedJob.getKey()).thenReturn(12345L);
        when(activatedJob.getType()).thenReturn("match-customer-with-dri");
    }

    @Test
    void handleJob_WithValidCustomerId_ShouldCompleteSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("customerId", 1L);
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);
        when(customerService.getCustomerById(1L)).thenReturn(testCustomer);
        when(employeeService.getEmployeeById(1L)).thenReturn(testEmployee);

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(customerService).getCustomerById(1L);
        verify(employeeService).getEmployeeById(1L);
        verify(jobClient).newCompleteCommand(12345L);
        verify(jobHistoryService).recordJobExecution(
                eq("match-customer-with-dri"),
                eq("12345"),
                eq("COMPLETED"),
                anyString(),
                isNull(),
                anyLong()
        );
    }

    @Test
    void handleJob_WithInvalidCustomerId_ShouldFailJob() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("customerId", 999L);
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);
        when(customerService.getCustomerById(999L))
                .thenThrow(new RuntimeException("Customer not found"));

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(customerService).getCustomerById(999L);
        verify(jobClient).newFailCommand(12345L);
        verify(jobHistoryService).recordJobExecution(
                eq("match-customer-with-dri"),
                eq("12345"),
                eq("FAILED"),
                anyString(),
                eq("Customer not found"),
                anyLong()
        );
    }

    @Test
    void handleJob_WithMissingCustomerId_ShouldFailJob() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("otherField", "value");
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(jobClient).newFailCommand(12345L);
        verify(jobHistoryService).recordJobExecution(
                eq("match-customer-with-dri"),
                eq("12345"),
                eq("FAILED"),
                anyString(),
                anyString(),
                anyLong()
        );
    }

    @Test
    void handleJob_WithInvalidCustomerIdType_ShouldFailJob() throws Exception {
        // Arrange
        Map<String, Object> variables = Map.of("customerId", "invalid");
        when(activatedJob.getVariablesAsMap()).thenReturn(variables);

        // Act
        worker.handleJob(jobClient, activatedJob);

        // Assert
        verify(jobClient).newFailCommand(12345L);
        verify(jobHistoryService).recordJobExecution(
                eq("match-customer-with-dri"),
                eq("12345"),
                eq("FAILED"),
                anyString(),
                anyString(),
                anyLong()
        );
    }
}
