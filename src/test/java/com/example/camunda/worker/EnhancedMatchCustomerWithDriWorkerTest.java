package com.example.camunda.worker;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.service.CustomerService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@ExtendWith(MockitoExtension.class)
public class EnhancedMatchCustomerWithDriWorkerTest {

    @Mock(lenient = true)  // Use lenient to avoid strict stubbing issues
    private CustomerService customerService;

    @Mock
    private ActivatedJob job;

    @InjectMocks
    private MatchCustomerWithDriWorker worker;

    private Customer testCustomer;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(123L);
        testCustomer.setCustomerName("Test Customer");
        testCustomer.setEmployeeId(456L);

        testEmployee = new Employee();
        testEmployee.setEmployeeId(456L);
        testEmployee.setFullName("John Doe");
        testEmployee.setJobTitle("Account Manager");
        testEmployee.setDepartment("Sales");
        testEmployee.setPhoneNumber("+1-555-0123");
    }

    @Test
    void handleJob_withValidCustomerId_shouldReturnConsolidatedResult() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", 123L);
        variables.put("customerName", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(customerService.getCustomerWithEmployee(eq(123L), eq(null), isNull())).thenReturn(testCustomer);
        when(customerService.getEmployeeForCustomer(testCustomer)).thenReturn(testEmployee);

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("matchStatus"));
        assertTrue(result.containsKey("matchingResult"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> matchingResult = (Map<String, Object>) result.get("matchingResult");
        
        assertEquals("SUCCESS", matchingResult.get("status"));
        assertTrue(matchingResult.containsKey("customers"));
        assertEquals(1, result.get("customerCount"));
        
        // Check individual fields for backward compatibility (single customer)
        assertEquals(123L, result.get("customerId"));
        assertEquals("Test Customer", result.get("customerName"));
        assertEquals("John Doe", result.get("employeeName"));
        assertEquals("Account Manager", result.get("employeeTitle"));
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> customers = (java.util.List<Map<String, Object>>) result.get("customers");
        assertEquals(1, customers.size());
        
        Map<String, Object> customerPair = customers.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) customerPair.get("customer");
        @SuppressWarnings("unchecked")
        Map<String, Object> employee = (Map<String, Object>) customerPair.get("employee");
        
        assertEquals(123L, customer.get("customerId"));
        assertEquals("Test Customer", customer.get("customerName"));
        assertEquals("John Doe", employee.get("fullName"));
        assertEquals("Account Manager", employee.get("jobTitle"));
    }

    @Test
    void handleJob_withValidCustomerName_shouldReturnConsolidatedResult() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", null);
        variables.put("customerName", "Test Customer");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(customerService.getCustomerWithEmployee(eq(null), eq("Test Customer"), isNull())).thenReturn(testCustomer);
        when(customerService.getEmployeeForCustomer(testCustomer)).thenReturn(testEmployee);

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("matchStatus"));
        @SuppressWarnings("unchecked")
        Map<String, Object> matchingResult = (Map<String, Object>) result.get("matchingResult");
        assertEquals("SUCCESS", matchingResult.get("status"));

        @SuppressWarnings("unchecked")
        Map<String, Object> searchParams = (Map<String, Object>) matchingResult.get("searchParameters");
        assertEquals("Test Customer", searchParams.get("customerName"));
        assertFalse(searchParams.containsKey("customerId"));
    }

    @Test
    void handleJob_withBothParameters_shouldReturnConsolidatedResult() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", 123L);
        variables.put("customerName", "Test Customer");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(customerService.getCustomerWithEmployee(eq(123L), eq("Test Customer"), isNull())).thenReturn(testCustomer);
        when(customerService.getEmployeeForCustomer(testCustomer)).thenReturn(testEmployee);

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertEquals("SUCCESS", result.get("matchStatus"));
        @SuppressWarnings("unchecked")
        Map<String, Object> matchingResult = (Map<String, Object>) result.get("matchingResult");
        @SuppressWarnings("unchecked")
        Map<String, Object> searchParams = (Map<String, Object>) matchingResult.get("searchParameters");
        
        assertEquals(123L, searchParams.get("customerId"));
        assertEquals("Test Customer", searchParams.get("customerName"));
    }

    @Test
    void handleJob_withNullParameters_shouldReturnValidationError() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", null);
        variables.put("customerName", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);

        // Act
        Map<String, Object> result = worker.handleJob(job);
        
        // Assert
        assertNotNull(result);
        assertEquals("VALIDATION_ERROR", result.get("matchStatus"));
        assertEquals(0, result.get("customerCount"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> matchingResult = (Map<String, Object>) result.get("matchingResult");
        assertEquals("VALIDATION_ERROR", matchingResult.get("status"));
        assertTrue(((String) matchingResult.get("error")).contains("At least one search parameter"));
    }

    @Test
    void handleJob_withEmptyStringParameters_shouldReturnValidationError() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", null);
        variables.put("customerName", "   ");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);

        // Act
        Map<String, Object> result = worker.handleJob(job);
        
        // Assert
        assertNotNull(result);
        assertEquals("VALIDATION_ERROR", result.get("matchStatus"));
        assertEquals(0, result.get("customerCount"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> matchingResult = (Map<String, Object>) result.get("matchingResult");
        assertEquals("VALIDATION_ERROR", matchingResult.get("status"));
        assertTrue(((String) matchingResult.get("error")).contains("At least one search parameter"));
    }

    @Test
    void handleJob_withStringNumberId_shouldParseCorrectly() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", "123");
        variables.put("customerName", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(customerService.getCustomerWithEmployee(eq(123L), eq(null), isNull())).thenReturn(testCustomer);
        when(customerService.getEmployeeForCustomer(testCustomer)).thenReturn(testEmployee);

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("matchStatus"));
        verify(customerService).getCustomerWithEmployee(eq(123L), eq(null), isNull());
    }

    @Test
    void handleJob_withAllowMultiple_shouldReturnMultipleResults() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", null);
        variables.put("customerName", "John");
        variables.put("allowMultiple", true);
        
        Customer testCustomer2 = new Customer();
        testCustomer2.setCustomerId(124L);
        testCustomer2.setCustomerName("John Smith");
        testCustomer2.setEmployeeId(457L);
        
        Employee testEmployee2 = new Employee();
        testEmployee2.setEmployeeId(457L);
        testEmployee2.setFullName("Jane Wilson");
        testEmployee2.setJobTitle("Sales Rep");
        testEmployee2.setDepartment("Sales");
        testEmployee2.setPhoneNumber("+1-555-0124");

        java.util.List<Customer> customers = java.util.Arrays.asList(testCustomer, testCustomer2);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(customerService.getCustomersWithEmployees(eq(null), eq("John"), isNull())).thenReturn(customers);
        when(customerService.getEmployeeForCustomer(testCustomer)).thenReturn(testEmployee);
        when(customerService.getEmployeeForCustomer(testCustomer2)).thenReturn(testEmployee2);

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("matchStatus"));
        assertEquals(2, result.get("customerCount"));
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> customerPairs = (java.util.List<Map<String, Object>>) result.get("customers");
        assertEquals(2, customerPairs.size());
        
        // Individual fields should be null when multiple customers
        assertNull(result.get("customerId"));
        assertNull(result.get("customerName"));
        assertNull(result.get("employeeName"));
        
        verify(customerService).getCustomersWithEmployees(eq(null), eq("John"), isNull());
    }
}
