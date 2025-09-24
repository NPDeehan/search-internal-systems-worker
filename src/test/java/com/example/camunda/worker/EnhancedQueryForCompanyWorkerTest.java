package com.example.camunda.worker;

import com.example.camunda.model.ExternalCompany;
import com.example.camunda.service.CompanyService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnhancedQueryForCompanyWorkerTest {

    @Mock
    private CompanyService companyService;

    @Mock
    private ActivatedJob job;

    @InjectMocks
    private QueryForCompanyWorker worker;

    private ExternalCompany testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new ExternalCompany();
        testCompany.setCompanyId(123L);
        testCompany.setCompanyName("Test Company");
        testCompany.setAddress("123 Tech Street, San Francisco, CA");
        testCompany.setContactPerson("John Doe");
        testCompany.setPhoneNumber("+1-555-0123");
        testCompany.setCreatedAt(LocalDateTime.now());
        testCompany.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void handleJob_withValidCompanyName_shouldReturnConsolidatedResult() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", "Test Company");
        variables.put("industry", null);
        variables.put("city", null);
        variables.put("revenue", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(companyService.findCompany("Test Company", null, null, null))
            .thenReturn(List.of(testCompany));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("companySearchResult"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("companySearchResult");
        
        assertEquals("SUCCESS", searchResult.get("status"));
        assertNotNull(searchResult.get("companies"));
        assertNotNull(searchResult.get("timestamp"));
        assertNotNull(searchResult.get("searchParameters"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> companies = (List<Map<String, Object>>) searchResult.get("companies");
        assertEquals(1, companies.size());
        
        Map<String, Object> company = companies.get(0);
        assertEquals(123L, company.get("companyId"));
        assertEquals("Test Company", company.get("companyName"));
        assertEquals("123 Tech Street, San Francisco, CA", company.get("address"));

        @SuppressWarnings("unchecked")
        Map<String, Object> searchParams = (Map<String, Object>) searchResult.get("searchParameters");
        assertEquals("Test Company", searchParams.get("companyName"));
        assertFalse(searchParams.containsKey("industry"));
        assertFalse(searchParams.containsKey("city"));
        assertFalse(searchParams.containsKey("revenue"));
    }

    @Test
    void handleJob_withMultipleParameters_shouldReturnConsolidatedResult() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", "Test Company");
        variables.put("industry", "Technology");
        variables.put("city", "San Francisco");
        variables.put("revenue", 1000000L);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(companyService.findCompany("Test Company", "Technology", "San Francisco", 1000000L))
            .thenReturn(List.of(testCompany));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("companySearchResult");
        @SuppressWarnings("unchecked")
        Map<String, Object> searchParams = (Map<String, Object>) searchResult.get("searchParameters");
        
        assertEquals("Test Company", searchParams.get("companyName"));
        assertEquals("Technology", searchParams.get("industry"));
        assertEquals("San Francisco", searchParams.get("city"));
        assertEquals(1000000L, searchParams.get("revenue"));
    }

    @Test
    void handleJob_withPartialParameters_shouldUseOnlyNonNull() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", null);
        variables.put("industry", "Technology");
        variables.put("city", null);
        variables.put("revenue", 1000000L);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(companyService.findCompany(null, "Technology", null, 1000000L))
            .thenReturn(List.of(testCompany));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("companySearchResult");
        @SuppressWarnings("unchecked")
        Map<String, Object> searchParams = (Map<String, Object>) searchResult.get("searchParameters");
        
        assertEquals("Technology", searchParams.get("industry"));
        assertEquals(1000000L, searchParams.get("revenue"));
        assertFalse(searchParams.containsKey("companyName"));
        assertFalse(searchParams.containsKey("city"));

        verify(companyService).findCompany(null, "Technology", null, 1000000L);
    }

    @Test
    void handleJob_withNullParameters_shouldThrowException() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", null);
        variables.put("industry", null);
        variables.put("city", null);
        variables.put("revenue", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> worker.handleJob(job)
        );
        
        assertTrue(exception.getMessage().contains("At least one search parameter"));
    }

    @Test
    void handleJob_withEmptyStringParameters_shouldThrowException() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", "   ");
        variables.put("industry", "");
        variables.put("city", null);
        variables.put("revenue", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> worker.handleJob(job)
        );
        
        assertTrue(exception.getMessage().contains("At least one search parameter"));
    }

    @Test
    void handleJob_withStringRevenue_shouldParseCorrectly() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", "Test Company");
        variables.put("industry", null);
        variables.put("city", null);
        variables.put("revenue", "1000000");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(companyService.findCompany("Test Company", null, null, 1000000L))
            .thenReturn(List.of(testCompany));

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        assertNotNull(result);
        verify(companyService).findCompany("Test Company", null, null, 1000000L);
    }

    @Test
    void handleJob_withNoResults_shouldReturnEmptyList() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", "Non-existent Company");
        variables.put("industry", null);
        variables.put("city", null);
        variables.put("revenue", null);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(12345L);
        when(companyService.findCompany("Non-existent Company", null, null, null))
            .thenReturn(List.of());

        // Act
        Map<String, Object> result = worker.handleJob(job);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>) result.get("companySearchResult");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> companies = (List<Map<String, Object>>) searchResult.get("companies");
        
        assertEquals("NOT_FOUND", searchResult.get("status"));
        assertTrue(companies.isEmpty());
    }
}
