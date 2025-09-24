package com.example.camunda.service;

import com.example.camunda.model.Employee;
import com.example.camunda.repository.EmployeeRepository;
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
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        log.debug("Fetching all employees");
        return employeeRepository.findAll();
    }

    public Optional<Employee> findEmployeeById(Long employeeId) {
        log.debug("Finding employee by ID: {}", employeeId);
        return employeeRepository.findById(employeeId);
    }

    public Employee getEmployeeById(Long employeeId) {
        return findEmployeeById(employeeId)
                .orElseThrow(() -> new com.example.camunda.exception.EmployeeNotFoundException(
                    "Employee not found with ID: " + employeeId));
    }

    public Optional<Employee> findEmployeeByName(String fullName) {
        log.debug("Finding employee by exact name: '{}'", fullName);
        if (fullName == null || fullName.trim().isEmpty()) {
            return Optional.empty();
        }
        return employeeRepository.findByFullName(fullName.trim());
    }

    public List<Employee> searchEmployeesByName(String fullName) {
        log.debug("Searching employees by name containing: '{}'", fullName);
        if (fullName == null || fullName.trim().isEmpty()) {
            return List.of();
        }
        return employeeRepository.findByFullNameContainingIgnoreCase(fullName.trim());
    }

    public List<Employee> searchEmployees(String fullName, String department, String jobTitle) {
        log.debug("Searching employees - Name: '{}', Department: '{}', JobTitle: '{}'", 
                 fullName, department, jobTitle);
        
        // Validate that at least one search parameter is provided
        if ((fullName == null || fullName.trim().isEmpty()) && 
            (department == null || department.trim().isEmpty()) &&
            (jobTitle == null || jobTitle.trim().isEmpty())) {
            log.warn("No valid search parameters provided for employee search");
            return List.of();
        }
        
        return employeeRepository.searchEmployees(
            fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : null,
            department != null && !department.trim().isEmpty() ? department.trim() : null,
            jobTitle != null && !jobTitle.trim().isEmpty() ? jobTitle.trim() : null
        );
    }

    public List<Employee> searchEmployeesFuzzy(String fullName, String department, String jobTitle) {
        log.debug("Fuzzy searching employees - Name: '{}', Department: '{}', JobTitle: '{}'", 
                 fullName, department, jobTitle);
        
        // Validate that at least one search parameter is provided
        if ((fullName == null || fullName.trim().isEmpty()) && 
            (department == null || department.trim().isEmpty()) &&
            (jobTitle == null || jobTitle.trim().isEmpty())) {
            log.warn("No valid search parameters provided for employee fuzzy search");
            return List.of();
        }
        
        List<Employee> allEmployees = employeeRepository.findAll();
        
        String lowerFullName = fullName != null ? fullName.toLowerCase().trim() : null;
        String lowerDepartment = department != null ? department.toLowerCase().trim() : null;
        String lowerJobTitle = jobTitle != null ? jobTitle.toLowerCase().trim() : null;
        
        List<Employee> fuzzyResults = allEmployees.stream()
            .filter(employee -> {
                boolean matches = false;
                
                // Fuzzy match on full name
                if (lowerFullName != null && employee.getFullName() != null) {
                    String empName = employee.getFullName().toLowerCase();
                    if (isFuzzyMatch(lowerFullName, empName)) {
                        matches = true;
                    }
                }
                
                // Fuzzy match on department
                if (lowerDepartment != null && employee.getDepartment() != null) {
                    String empDept = employee.getDepartment().toLowerCase();
                    if (isFuzzyMatch(lowerDepartment, empDept)) {
                        matches = true;
                    }
                }
                
                // Fuzzy match on job title
                if (lowerJobTitle != null && employee.getJobTitle() != null) {
                    String empTitle = employee.getJobTitle().toLowerCase();
                    if (isFuzzyMatch(lowerJobTitle, empTitle)) {
                        matches = true;
                    }
                }
                
                return matches;
            })
            .toList();
        
        log.info("Fuzzy search returned {} employees", fuzzyResults.size());
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

    @Transactional
    public Employee saveEmployee(Employee employee) {
        log.info("Saving employee: {}", employee.getFullName());
        return employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(Long employeeId) {
        log.info("Deleting employee with ID: {}", employeeId);
        if (!employeeRepository.existsById(employeeId)) {
            throw new com.example.camunda.exception.EmployeeNotFoundException("Employee not found with ID: " + employeeId);
        }
        employeeRepository.deleteById(employeeId);
    }
}
