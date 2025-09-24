package com.example.camunda.worker;

import com.example.camunda.model.ExternalCompany;
import com.example.camunda.service.CompanyService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueryForCompanyWorker {
    
    private final CompanyService companyService;

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing query-for-company job: {}", job.getKey());
        
        Map<String, Object> variables = job.getVariablesAsMap();
        String companyName = extractString(variables.get("companyName"));
        String industry = extractString(variables.get("industry"));
        String city = extractString(variables.get("city"));
        Long revenue = extractLong(variables.get("revenue"));
        Boolean fuzzyMatching = extractBoolean(variables.get("fuzzyMatching"));
        
        log.info("Querying companies - Name: {}, Industry: {}, City: {}, Revenue: {}, Fuzzy: {}", 
                 companyName, industry, city, revenue, fuzzyMatching);
        
        // Validate that at least one parameter is provided
        if ((companyName == null || companyName.trim().isEmpty()) && 
            (industry == null || industry.trim().isEmpty()) &&
            (city == null || city.trim().isEmpty()) &&
            revenue == null) {
            throw new IllegalArgumentException("At least one search parameter must be provided");
        }
        
        // Find companies using enhanced service layer
        List<ExternalCompany> companies;
        
        if (fuzzyMatching != null && fuzzyMatching) {
            companies = companyService.findCompanyFuzzy(companyName, industry, city, revenue);
        } else {
            companies = companyService.findCompany(companyName, industry, city, revenue);
        }
        
        // Create consolidated result object
        Map<String, Object> queryResult = new HashMap<>();
        
        if (companies.isEmpty()) {
            // Handle "not found" case
            log.info("No company records found with search criteria - Name: {}, Industry: {}, City: {}, Revenue: {}", 
                     companyName, industry, city, revenue);
            
            queryResult.put("status", "NOT_FOUND");
            queryResult.put("message", "No company records could be found with the provided search criteria");
            queryResult.put("companies", List.of());
            queryResult.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // Include search parameters used (only non-null ones)
            Map<String, Object> searchParams = new HashMap<>();
            if (companyName != null && !companyName.trim().isEmpty()) searchParams.put("companyName", companyName);
            if (industry != null && !industry.trim().isEmpty()) searchParams.put("industry", industry);
            if (city != null && !city.trim().isEmpty()) searchParams.put("city", city);
            if (revenue != null) searchParams.put("revenue", revenue);
            if (fuzzyMatching != null) searchParams.put("fuzzyMatching", fuzzyMatching);
            queryResult.put("searchParameters", searchParams);
            
            Map<String, Object> result = new HashMap<>();
            result.put("companySearchResult", queryResult);
            
            return result;
        }
        
        // Convert companies to response format
        List<Map<String, Object>> companyDataList = companies.stream()
            .map(company -> {
                Map<String, Object> companyData = new HashMap<>();
                companyData.put("companyId", company.getCompanyId());
                companyData.put("companyName", company.getCompanyName());
                companyData.put("address", company.getAddress() != null ? company.getAddress() : "");
                companyData.put("contactPerson", company.getContactPerson() != null ? company.getContactPerson() : "");
                companyData.put("phoneNumber", company.getPhoneNumber() != null ? company.getPhoneNumber() : "");
                return companyData;
            })
            .collect(Collectors.toList());
        
        // Build consolidated response
        queryResult.put("status", "SUCCESS");
        queryResult.put("companies", companyDataList);
        queryResult.put("timestamp", java.time.LocalDateTime.now().toString());
        
        // Include search parameters used (only non-null ones)
        Map<String, Object> searchParams = new HashMap<>();
        if (companyName != null && !companyName.trim().isEmpty()) searchParams.put("companyName", companyName);
        if (industry != null && !industry.trim().isEmpty()) searchParams.put("industry", industry);
        if (city != null && !city.trim().isEmpty()) searchParams.put("city", city);
        if (revenue != null) searchParams.put("revenue", revenue);
        if (fuzzyMatching != null) searchParams.put("fuzzyMatching", fuzzyMatching);
        queryResult.put("searchParameters", searchParams);
        
        Map<String, Object> result = new HashMap<>();
        result.put("companySearchResult", queryResult);
        
        log.info("Successfully found {} companies with search criteria", companies.size());
        
        return result;
    }
    
    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            String str = (String) value;
            if (str.trim().isEmpty()) return null;
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractString(Object value) {
        if (value == null) return null;
        if (value instanceof String) {
            String str = (String) value;
            return str.trim().isEmpty() ? null : str.trim();
        }
        return value.toString().trim();
    }
    
    private Boolean extractBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.trim().isEmpty()) {
                return null;
            }
            return "true".equalsIgnoreCase(str.trim()) || "1".equals(str.trim()) || "yes".equalsIgnoreCase(str.trim());
        }
        return null;
    }
}
