package com.example.camunda.service;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.exception.CustomerNotFoundException;
import com.example.camunda.exception.EmployeeNotFoundException;
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
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public List<Customer> getAllCustomers() {
        log.debug("Fetching all customers");
        return customerRepository.findAll();
    }

    public Optional<Customer> findCustomer(Long customerId, String customerName) {
        log.debug("Finding customer by ID: {} or name: '{}'", customerId, customerName);
        
        // Validate that at least one search parameter is provided
        if (customerId == null && (customerName == null || customerName.trim().isEmpty())) {
            log.warn("No valid search parameters provided for customer search");
            return Optional.empty();
        }
        
        Optional<Customer> result = Optional.empty();
        
        if (customerId != null && customerName != null && !customerName.trim().isEmpty()) {
            log.debug("Searching by both ID and name: {} / '{}'", customerId, customerName);
            result = customerRepository.findByCustomerIdOrCustomerName(customerId, customerName);
        } else if (customerId != null) {
            log.debug("Searching by ID only: {}", customerId);
            result = customerRepository.findByCustomerId(customerId);
        } else if (customerName != null && !customerName.trim().isEmpty()) {
            log.debug("Searching by name only: '{}'", customerName);
            result = customerRepository.findByCustomerName(customerName);
            
            // If exact match fails, try partial matching as fallback
            if (result.isEmpty()) {
                log.debug("Exact name match failed, trying partial match for: '{}'", customerName);
                List<Customer> allCustomers = customerRepository.findAll();
                result = allCustomers.stream()
                    .filter(customer -> customer.getCustomerName() != null && 
                           customer.getCustomerName().toLowerCase().contains(customerName.toLowerCase().trim()))
                    .findFirst();
                
                if (result.isPresent()) {
                    log.info("Found customer by partial name match: '{}' matched '{}'", 
                           customerName, result.get().getCustomerName());
                }
            }
        }
        
        if (result.isPresent()) {
            log.debug("Found customer: {} (ID: {})", result.get().getCustomerName(), result.get().getCustomerId());
        } else {
            log.warn("No customer found with search criteria - ID: {}, Name: '{}'", customerId, customerName);
            
            // Log some existing customers for debugging
            List<Customer> allCustomers = customerRepository.findAll();
            log.debug("Available customers in database: {}", 
                allCustomers.stream()
                    .limit(5)  // Show first 5 customers
                    .map(c -> String.format("%s (ID: %d)", c.getCustomerName(), c.getCustomerId()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
            
            if (allCustomers.size() > 5) {
                log.debug("... and {} more customers", allCustomers.size() - 5);
            }
        }
        
        return result;
    }

    public List<Customer> findCustomers(Long customerId, String customerName) {
        log.debug("Finding customers (multiple) by ID: {} or name: '{}'", customerId, customerName);
        
        // Validate that at least one search parameter is provided
        if (customerId == null && (customerName == null || customerName.trim().isEmpty())) {
            log.warn("No valid search parameters provided for customer search");
            return List.of();
        }
        
        List<Customer> results = List.of();
        
        if (customerId != null && customerName != null && !customerName.trim().isEmpty()) {
            log.debug("Searching by both ID and name: {} / '{}'", customerId, customerName);
            // For exact match, still return single customer
            Optional<Customer> exactMatch = customerRepository.findByCustomerIdOrCustomerName(customerId, customerName);
            results = exactMatch.map(List::of).orElse(List.of());
        } else if (customerId != null) {
            log.debug("Searching by ID only: {}", customerId);
            Optional<Customer> exactMatch = customerRepository.findByCustomerId(customerId);
            results = exactMatch.map(List::of).orElse(List.of());
        } else if (customerName != null && !customerName.trim().isEmpty()) {
            log.debug("Searching by name: '{}'", customerName);
            
            // First try exact match
            Optional<Customer> exactMatch = customerRepository.findByCustomerName(customerName);
            if (exactMatch.isPresent()) {
                results = List.of(exactMatch.get());
            } else {
                // If exact match fails, try partial matching to get multiple results
                log.debug("Exact name match failed, trying partial match for: '{}'", customerName);
                List<Customer> allCustomers = customerRepository.findAll();
                results = allCustomers.stream()
                    .filter(customer -> customer.getCustomerName() != null && 
                           customer.getCustomerName().toLowerCase().contains(customerName.toLowerCase().trim()))
                    .toList();
                
                if (!results.isEmpty()) {
                    log.info("Found {} customers by partial name match for: '{}'", results.size(), customerName);
                }
            }
        }
        
        if (!results.isEmpty()) {
            log.debug("Found {} customer(s)", results.size());
        } else {
            log.warn("No customers found with search criteria - ID: {}, Name: '{}'", customerId, customerName);
        }
        
        return results;
    }

    public List<Customer> findCustomers(String customerName, Boolean allowMultiple) {
        log.debug("Finding customers with name: '{}', allowMultiple: {}", customerName, allowMultiple);
        
        if (customerName == null || customerName.trim().isEmpty()) {
            return List.of();
        }
        
        // First try exact match
        Optional<Customer> exactMatch = customerRepository.findByCustomerName(customerName.trim());
        if (exactMatch.isPresent()) {
            return List.of(exactMatch.get());
        }
        
        // If no exact match and multiple results allowed, do partial search
        if (allowMultiple != null && allowMultiple) {
            List<Customer> allCustomers = customerRepository.findAll();
            List<Customer> partialMatches = allCustomers.stream()
                .filter(customer -> customer.getCustomerName() != null && 
                       customer.getCustomerName().toLowerCase().contains(customerName.toLowerCase().trim()))
                .toList();
            
            log.debug("Partial search for '{}' returned {} results", customerName, partialMatches.size());
            return partialMatches;
        }
        
        return List.of();
    }

    public List<Customer> findCustomers(Long customerId, String customerName, Boolean fuzzyMatching) {
        log.debug("Finding customers (with fuzzy matching) by ID: {} or name: '{}', fuzzy: {}", 
                 customerId, customerName, fuzzyMatching);
        
        // Validate that at least one search parameter is provided
        if (customerId == null && (customerName == null || customerName.trim().isEmpty())) {
            log.warn("No valid search parameters provided for customer search");
            return List.of();
        }
        
        List<Customer> results = List.of();
        
        if (customerId != null && customerName != null && !customerName.trim().isEmpty()) {
            log.debug("Searching by both ID and name: {} / '{}'", customerId, customerName);
            // For exact match, still return single customer
            Optional<Customer> exactMatch = customerRepository.findByCustomerIdOrCustomerName(customerId, customerName);
            results = exactMatch.map(List::of).orElse(List.of());
            
            // If no exact match and fuzzy matching is enabled, try fuzzy search by name only
            if (results.isEmpty() && fuzzyMatching != null && fuzzyMatching) {
                results = performFuzzyCustomerSearch(customerName);
            }
        } else if (customerId != null) {
            log.debug("Searching by ID only: {}", customerId);
            Optional<Customer> exactMatch = customerRepository.findByCustomerId(customerId);
            results = exactMatch.map(List::of).orElse(List.of());
        } else if (customerName != null && !customerName.trim().isEmpty()) {
            log.debug("Searching by name: '{}'", customerName);
            
            // First try exact match
            Optional<Customer> exactMatch = customerRepository.findByCustomerName(customerName);
            if (exactMatch.isPresent()) {
                results = List.of(exactMatch.get());
            } else if (fuzzyMatching != null && fuzzyMatching) {
                // Use fuzzy matching
                results = performFuzzyCustomerSearch(customerName);
            } else {
                // Standard partial matching (existing behavior)
                log.debug("Exact name match failed, trying partial match for: '{}'", customerName);
                List<Customer> allCustomers = customerRepository.findAll();
                results = allCustomers.stream()
                    .filter(customer -> customer.getCustomerName() != null && 
                           customer.getCustomerName().toLowerCase().contains(customerName.toLowerCase().trim()))
                    .toList();
                
                if (!results.isEmpty()) {
                    log.info("Found {} customers by partial name match for: '{}'", results.size(), customerName);
                }
            }
        }
        
        if (!results.isEmpty()) {
            log.debug("Found {} customer(s)", results.size());
        } else {
            log.warn("No customers found with search criteria - ID: {}, Name: '{}'", customerId, customerName);
        }
        
        return results;
    }
    
    private List<Customer> performFuzzyCustomerSearch(String searchName) {
        log.debug("Performing fuzzy search for customer name: '{}'", searchName);
        
        List<Customer> allCustomers = customerRepository.findAll();
        String lowerSearchName = searchName.toLowerCase().trim();
        
        // Implement simple fuzzy matching logic
        List<Customer> fuzzyResults = allCustomers.stream()
            .filter(customer -> {
                if (customer.getCustomerName() == null) return false;
                
                String customerName = customer.getCustomerName().toLowerCase();
                
                // Exact match (highest priority)
                if (customerName.equals(lowerSearchName)) return true;
                
                // Contains match
                if (customerName.contains(lowerSearchName) || lowerSearchName.contains(customerName)) return true;
                
                // Word-based matching (check if any words match)
                String[] searchWords = lowerSearchName.split("\\s+");
                String[] customerWords = customerName.split("\\s+");
                
                for (String searchWord : searchWords) {
                    for (String customerWord : customerWords) {
                        // Exact word match
                        if (searchWord.equals(customerWord)) return true;
                        
                        // Partial word match (at least 3 characters and 70% similarity)
                        if (searchWord.length() >= 3 && customerWord.length() >= 3) {
                            double similarity = calculateStringSimilarity(searchWord, customerWord);
                            if (similarity > 0.7) return true;
                        }
                    }
                }
                
                return false;
            })
            .toList();
        
        log.info("Fuzzy search for '{}' returned {} results", searchName, fuzzyResults.size());
        return fuzzyResults;
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

    public List<Customer> getCustomersWithEmployees(Long customerId, String customerName) {
        List<Customer> customers = findCustomers(customerId, customerName);
        
        if (customers.isEmpty()) {
            // Get a few example customer names for the error message
            List<Customer> sampleCustomers = customerRepository.findAll().stream().limit(3).toList();
            String examples = sampleCustomers.stream()
                .map(Customer::getCustomerName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
                
            log.info("No customers found with ID: {}, Name: '{}'. Available customers include: {}", 
                     customerId, customerName, examples);
        }
        
        return customers;
    }

    public List<Customer> getCustomersWithEmployees(Long customerId, String customerName, Boolean fuzzyMatching) {
        List<Customer> customers = findCustomers(customerId, customerName, fuzzyMatching);
        
        if (customers.isEmpty()) {
            // Get a few example customer names for the error message
            List<Customer> sampleCustomers = customerRepository.findAll().stream().limit(3).toList();
            String examples = sampleCustomers.stream()
                .map(Customer::getCustomerName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
                
            log.info("No customers found with ID: {}, Name: '{}', fuzzy: {}. Available customers include: {}", 
                     customerId, customerName, fuzzyMatching, examples);
        }
        
        return customers;
    }

    public Customer getCustomerWithEmployee(Long customerId, String customerName, Boolean fuzzyMatching) {
        List<Customer> customers = findCustomers(customerId, customerName, fuzzyMatching);
        
        if (customers.isEmpty()) {
            // Get a few example customer names for the error message
            List<Customer> sampleCustomers = customerRepository.findAll().stream().limit(3).toList();
            String examples = sampleCustomers.stream()
                .map(Customer::getCustomerName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
                
            String errorMsg = String.format(
                "Customer not found with ID: %s, Name: '%s', fuzzy: %s. Available customers include: %s", 
                customerId, customerName, fuzzyMatching, examples);
            
            throw new CustomerNotFoundException(errorMsg);
        }
        
        Customer customer = customers.get(0); // Take first result for single customer search
        log.debug("Found customer: {}, fetching associated employee", customer.getCustomerName());
        return customer;
    }

    public Customer getCustomerWithEmployee(Long customerId, String customerName) {
        Optional<Customer> customerOpt = findCustomer(customerId, customerName);
        
        if (customerOpt.isEmpty()) {
            // Get a few example customer names for the error message
            List<Customer> sampleCustomers = customerRepository.findAll().stream().limit(3).toList();
            String examples = sampleCustomers.stream()
                .map(Customer::getCustomerName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
                
            String errorMsg = String.format(
                "Customer not found with ID: %s, Name: '%s'. Available customers include: %s", 
                customerId, customerName, examples);
            
            throw new CustomerNotFoundException(errorMsg);
        }
        
        Customer customer = customerOpt.get();
        log.debug("Found customer: {}, fetching associated employee", customer.getCustomerName());
        return customer;
    }

    public Employee getEmployeeForCustomer(Customer customer) {
        return employeeRepository.findById(customer.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(
                    String.format("Employee not found with ID: %s for customer: %s", 
                        customer.getEmployeeId(), customer.getCustomerName())));
    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        log.info("Saving customer: {}", customer.getCustomerName());
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(Long customerId) {
        log.info("Deleting customer with ID: {}", customerId);
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }
        customerRepository.deleteById(customerId);
    }
}
