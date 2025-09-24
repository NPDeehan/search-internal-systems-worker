package com.example.camunda.service;

import com.example.camunda.model.ExternalCompany;
import com.example.camunda.repository.ExternalCompanyRepository;
import com.example.camunda.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyService {
    
    private final ExternalCompanyRepository companyRepository;

    public List<ExternalCompany> getAllCompanies() {
        log.debug("Fetching all external companies");
        return companyRepository.findAll();
    }

    public List<ExternalCompany> findCompany(String companyName, String industry, String city, Long revenue) {
        log.debug("Finding companies by name: {}, industry: {}, city: {}, revenue: {}", 
                  companyName, industry, city, revenue);
        
        // Validate that at least one search parameter is provided
        if ((companyName == null || companyName.trim().isEmpty()) && 
            (industry == null || industry.trim().isEmpty()) &&
            (city == null || city.trim().isEmpty()) &&
            revenue == null) {
            log.warn("No valid search parameters provided for company search");
            return List.of();
        }
        
        // For now, use simple name-based search since we only have basic fields in ExternalCompany
        if (companyName != null && !companyName.trim().isEmpty()) {
            return companyRepository.findByCompanyName(companyName)
                    .map(List::of)
                    .orElse(List.of());
        }
        
        // If no name provided but other parameters exist, return all companies
        // In a real implementation, you'd have a more sophisticated query
        return companyRepository.findAll();
    }

    public List<ExternalCompany> findCompanyFuzzy(String companyName, String industry, String city, Long revenue) {
        log.debug("Fuzzy finding companies by name: {}, industry: {}, city: {}, revenue: {}", 
                  companyName, industry, city, revenue);
        
        // Validate that at least one search parameter is provided
        if ((companyName == null || companyName.trim().isEmpty()) && 
            (industry == null || industry.trim().isEmpty()) &&
            (city == null || city.trim().isEmpty()) &&
            revenue == null) {
            log.warn("No valid search parameters provided for company fuzzy search");
            return List.of();
        }
        
        List<ExternalCompany> allCompanies = companyRepository.findAll();
        
        String lowerCompanyName = companyName != null ? companyName.toLowerCase().trim() : null;
        String lowerIndustry = industry != null ? industry.toLowerCase().trim() : null;
        String lowerCity = city != null ? city.toLowerCase().trim() : null;
        
        List<ExternalCompany> fuzzyResults = allCompanies.stream()
            .filter(company -> {
                boolean matches = false;
                
                // Fuzzy match on company name
                if (lowerCompanyName != null && company.getCompanyName() != null) {
                    String compName = company.getCompanyName().toLowerCase();
                    if (isFuzzyMatch(lowerCompanyName, compName)) {
                        matches = true;
                    }
                }
                
                // Fuzzy match on address for city (since we don't have separate city field)
                if (lowerCity != null && company.getAddress() != null) {
                    String address = company.getAddress().toLowerCase();
                    if (isFuzzyMatch(lowerCity, address)) {
                        matches = true;
                    }
                }
                
                // Note: Industry and revenue matching would require additional fields in ExternalCompany
                // For now, we're focusing on name and city (via address) matching
                
                return matches;
            })
            .toList();
        
        log.info("Fuzzy company search returned {} results", fuzzyResults.size());
        return fuzzyResults;
    }
    
    private boolean isFuzzyMatch(String searchTerm, String fieldValue) {
        if (searchTerm == null || fieldValue == null) return false;
        
        // Exact match (highest priority)
        if (fieldValue.equals(searchTerm)) return true;
        
        // Contains match
        if (fieldValue.contains(searchTerm) || searchTerm.contains(fieldValue)) return true;
        
        // Word-based matching
        String[] searchWords = searchTerm.split("\\s+");
        String[] fieldWords = fieldValue.split("\\s+");
        
        for (String searchWord : searchWords) {
            for (String fieldWord : fieldWords) {
                // Exact word match
                if (searchWord.equals(fieldWord)) return true;
                
                // Partial word match (at least 3 characters and 70% similarity)
                if (searchWord.length() >= 3 && fieldWord.length() >= 3) {
                    double similarity = calculateStringSimilarity(searchWord, fieldWord);
                    if (similarity > 0.7) return true;
                }
            }
        }
        
        return false;
    }
    
    private double calculateStringSimilarity(String s1, String s2) {
        // Simple Levenshtein distance-based similarity
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    public Optional<ExternalCompany> findCompany(Long companyId, String companyName) {
        log.debug("Finding company by ID: {} or name: {}", companyId, companyName);
        
        // Validate that at least one search parameter is provided
        if (companyId == null && (companyName == null || companyName.trim().isEmpty())) {
            log.warn("No valid search parameters provided for company search");
            return Optional.empty();
        }
        
        if (companyId != null) {
            return companyRepository.findByCompanyId(companyId);
        } else if (companyName != null && !companyName.trim().isEmpty()) {
            return companyRepository.findByCompanyName(companyName);
        }
        
        return Optional.empty();
    }

    public ExternalCompany getCompany(Long companyId, String companyName) {
        return findCompany(companyId, companyName)
                .orElseThrow(() -> new CompanyNotFoundException(
                    String.format("Company not found with ID: %s, Name: %s", companyId, companyName)));
    }

    @Transactional
    public ExternalCompany saveCompany(ExternalCompany company) {
        log.info("Saving company: {}", company.getCompanyName());
        return companyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(Long companyId) {
        log.info("Deleting company with ID: {}", companyId);
        if (!companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException("Company not found with ID: " + companyId);
        }
        companyRepository.deleteById(companyId);
    }
}
