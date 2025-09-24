package com.example.camunda.worker;

import com.example.camunda.model.Employee;
import com.example.camunda.service.EmployeeService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeSearchWorkerTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private ActivatedJob job;

    @InjectMocks
    private EmployeeSearchWorker worker;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setEmployeeId(123L);
        testEmployee.setFullName("John Smith");
        testEmployee.setJobTitle("Software Engineer");
        testEmployee.setDepartment("IT");
        testEmployee.setPhoneNumber("+1-555-0123");
    }

    @Test
    void handleJob_withValidEmployeeName_shouldReturnEmployee() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", "John Smith");
        variables.put("exactMatch", true);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(employeeService.findEmployeeByName("John Smith")).thenReturn(java.util.Optional.of(testEmployee));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("searchStatus"));
        assertEquals(1, result.get("employeeCount"));
        assertEquals(123L, result.get("employeeId"));
        assertEquals("John Smith", result.get("employeeName"));
        assertEquals("Software Engineer", result.get("employeeTitle"));
        assertEquals("IT", result.get("employeeDepartment"));
        assertEquals("+1-555-0123", result.get("employeePhone"));

        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("employeeSearchResult");
        assertEquals("SUCCESS", searchResult.get("status"));
        assertEquals(1, searchResult.get("employeeCount"));
    }

    @Test
    void handleJob_withPartialNameMatch_shouldReturnEmployees() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", "John");
        variables.put("exactMatch", false);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(employeeService.searchEmployees("John", null, null)).thenReturn(List.of(testEmployee));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("searchStatus"));
        assertEquals(1, result.get("employeeCount"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> employees = (List<Map<String, Object>>) result.get("employees");
        assertEquals(1, employees.size());
        assertEquals("John Smith", employees.get(0).get("fullName"));
    }

    @Test
    void handleJob_withNoEmployeeFound_shouldReturnNotFound() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", "Unknown Employee");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(employeeService.searchEmployees("Unknown Employee", null, null)).thenReturn(List.of());

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("NOT_FOUND", result.get("searchStatus"));
        assertEquals(0, result.get("employeeCount"));
        assertNull(result.get("employeeId"));
        assertNull(result.get("employeeName"));

        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("employeeSearchResult");
        assertEquals("NOT_FOUND", searchResult.get("status"));
        assertTrue(searchResult.get("message").toString().contains("No employee records could be found"));
    }

    @Test
    void handleJob_withNoSearchParameters_shouldReturnValidationError() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("VALIDATION_ERROR", result.get("searchStatus"));
        assertEquals(0, result.get("employeeCount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("employeeSearchResult");
        assertEquals("VALIDATION_ERROR", searchResult.get("status"));
        assertTrue(searchResult.get("message").toString().contains("At least one search parameter"));
    }

    @Test
    void handleJob_withMultipleSearchCriteria_shouldReturnResults() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("department", "IT");
        variables.put("jobTitle", "Engineer");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(employeeService.searchEmployees(null, "IT", "Engineer")).thenReturn(List.of(testEmployee));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("searchStatus"));
        assertEquals(1, result.get("employeeCount"));
        
        verify(employeeService).searchEmployees(null, "IT", "Engineer");
    }
}
