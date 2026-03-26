package com.example.camunda.controller;

import com.example.camunda.dto.JobHistoryDTO;
import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.model.JobHistory;
import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.service.CustomerService;
import com.example.camunda.service.EmployeeService;
import com.example.camunda.service.CompanyService;
import com.example.camunda.service.CustomerAccountLinkService;
import com.example.camunda.service.JobHistoryService;
import com.example.camunda.service.ZeebeConnectionService;
import com.example.camunda.service.AccountService;
import com.example.camunda.ZeebeJobPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DataController {
    
    private static final Logger log = LoggerFactory.getLogger(DataController.class);
    
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final CompanyService companyService;
    private final AccountService accountService;
    private final CustomerAccountLinkService customerAccountLinkService;
    private final JobHistoryService jobHistoryService;
    private final ZeebeConnectionService zeebeConnectionService;
    private final ZeebeJobPollingService pollingService;

    public DataController(CustomerService customerService,
                         EmployeeService employeeService,
                         CompanyService companyService,
                         AccountService accountService,
                         CustomerAccountLinkService customerAccountLinkService,
                         JobHistoryService jobHistoryService,
                         ZeebeConnectionService zeebeConnectionService,
                         ZeebeJobPollingService pollingService) {
        this.customerService = customerService;
        this.employeeService = employeeService;
        this.companyService = companyService;
        this.accountService = accountService;
        this.customerAccountLinkService = customerAccountLinkService;
        this.jobHistoryService = jobHistoryService;
        this.zeebeConnectionService = zeebeConnectionService;
        this.pollingService = pollingService;
    }

    @GetMapping("/customers")
    public List<Customer> getCustomers() {
        log.debug("Fetching all customers");
        return customerService.getAllCustomers();
    }

    @GetMapping("/customers/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        log.debug("Fetching customer with ID: {}", id);
        return customerService.getCustomerById(id);
    }

    @GetMapping("/employees")
    public List<Employee> getEmployees() {
        log.debug("Fetching all employees");
        return employeeService.getAllEmployees();
    }

    @GetMapping("/employees/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        log.debug("Fetching employee with ID: {}", id);
        return employeeService.getEmployeeById(id);
    }

    @GetMapping("/companies")
    public List<ExternalCompany> getCompanies() {
        log.debug("Fetching all companies");
        return companyService.getAllCompanies();
    }

    @GetMapping("/companies/{id}")
    public ExternalCompany getCompanyById(@PathVariable Long id) {
        log.debug("Fetching company with ID: {}", id);
        return companyService.getCompanyById(id);
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> getAccounts() {
        log.debug("Fetching all accounts with roles");
        return accountService.getAllAccountsWithRoles().stream()
                .map(match -> {
                    Account account = match.account();
                    Map<String, Object> map = new HashMap<>();
                    map.put("accountId", account.getAccountId());
                    map.put("accountNumber", account.getAccountNumber());
                    map.put("accountType", account.getAccountType() != null ? account.getAccountType().name() : "");
                    map.put("balance", account.getBalance());
                    map.put("currency", account.getCurrency());
                    map.put("status", account.getStatus());
                    map.put("interestRate", account.getInterestRate());
                    map.put("openedDate", account.getOpenedDate());
                    map.put("lastActivityDate", account.getLastActivityDate());
                    String roles = match.roles().stream()
                            .map(r -> "%s (%s)".formatted(r.customerName(), r.role() != null ? r.role().name() : ""))
                            .collect(Collectors.joining(", "));
                    map.put("roles", roles);
                    return map;
                })
                .toList();
    }

    @GetMapping("/accounts/{id}")
    public Account getAccountById(@PathVariable Long id) {
        log.debug("Fetching account with ID: {}", id);
        return accountService.getAccountById(id);
    }

    @GetMapping("/customer-accounts")
    public List<Map<String, Object>> getCustomerAccounts() {
        log.debug("Fetching all customer-account links");
        return accountService.getAllCustomerAccountLinks().stream()
                .map(link -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", link.getId());
                    map.put("customerId", link.getCustomer() != null ? link.getCustomer().getCustomerId() : null);
                    map.put("customerName", link.getCustomer() != null ? link.getCustomer().getCustomerName() : "");
                    map.put("accountId", link.getAccount() != null ? link.getAccount().getAccountId() : null);
                    map.put("accountNumber", link.getAccount() != null ? link.getAccount().getAccountNumber() : "");
                    map.put("role", link.getRole() != null ? link.getRole().name() : "");
                    map.put("createdAt", link.getCreatedAt());
                    map.put("updatedAt", link.getUpdatedAt());
                    return map;
                })
                .toList();
    }

    @PostMapping("/customer-accounts")
    public Map<String, Object> createCustomerAccountLink(@RequestBody Map<String, Object> request) {
        Long accountId = extractRequiredLong(request.get("accountId"), "accountId");
        Long customerId = extractRequiredLong(request.get("customerId"), "customerId");
        AccountRole role = extractRequiredRole(request.get("role"));

        log.info("Creating/upserting customer-account link accountId={}, customerId={}, role={}", accountId, customerId, role);
        CustomerAccountLinkService.UpsertResult upsertResult = customerAccountLinkService.upsertRole(accountId, customerId, role);
        return buildCustomerAccountLinkResponse(upsertResult.link(), upsertResult.created() ? "CREATED" : "UPDATED");
    }

    @PutMapping("/customer-accounts")
    public Map<String, Object> updateCustomerAccountLinkRole(@RequestBody Map<String, Object> request) {
        Long accountId = extractRequiredLong(request.get("accountId"), "accountId");
        Long customerId = extractRequiredLong(request.get("customerId"), "customerId");
        AccountRole role = extractRequiredRole(request.get("role"));

        log.info("Updating/upserting customer-account link role accountId={}, customerId={}, role={}", accountId, customerId, role);
        CustomerAccountLinkService.UpsertResult upsertResult = customerAccountLinkService.upsertRole(accountId, customerId, role);
        return buildCustomerAccountLinkResponse(upsertResult.link(), upsertResult.created() ? "CREATED" : "UPDATED");
    }

    @DeleteMapping("/customer-accounts/{accountId}/{customerId}")
    public Map<String, String> deleteCustomerAccountLink(@PathVariable Long accountId, @PathVariable Long customerId) {
        log.info("Removing customer-account link accountId={}, customerId={}", accountId, customerId);
        boolean removed = customerAccountLinkService.removeLink(accountId, customerId);
        if (!removed) {
            throw new IllegalArgumentException("Customer-account link not found for accountId=" + accountId + " and customerId=" + customerId);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Customer-account link removed successfully");
        return response;
    }

    @GetMapping("/connection-status")
    public Map<String, Object> getConnectionStatus() {
        log.debug("Checking connection status");
        Map<String, Object> status = new HashMap<>();
        
        boolean connected = zeebeConnectionService.isConnected();
        status.put("connected", connected);
        
        if (!connected) {
            String error = zeebeConnectionService.getLastError();
            if (error != null) {
                status.put("error", error);
            }
        }
        
        return status;
    }

    @GetMapping("/worker-status")
    public Map<String, Object> getWorkerStatus() {
        log.debug("Fetching worker status");
        Map<String, Object> status = new HashMap<>();
        
        boolean isPolling = pollingService.isPollingActive();
        String workerStatus = isPolling ? "RUNNING" : "STOPPED";
        
        status.put("match-customer-with-dri", workerStatus);
        status.put("query-for-company", workerStatus);
        status.put("search-employee", workerStatus);
        status.put("search-account", workerStatus);
        status.put("manage-account-record", workerStatus);
        status.put("manage-customer-record", workerStatus);
        status.put("manage-employee-record", workerStatus);
        status.put("manage-company-record", workerStatus);
        status.put("manage-customer-account-link", workerStatus);
        
        return status;
    }

    @GetMapping("/job-history")
    public List<JobHistoryDTO> getJobHistory(@RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        log.debug("Fetching enhanced job history, limit={}", safeLimit);
        List<JobHistory> jobHistories = jobHistoryService.getRecentJobHistory(safeLimit);
        return jobHistories.stream()
                .map(JobHistoryDTO::fromJobHistory)
                .collect(Collectors.toList());
    }

    @GetMapping("/job-metrics")
    public Map<String, Object> getJobMetrics() {
        log.debug("Fetching job metrics");
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("totalJobs", jobHistoryService.getTotalJobsProcessed());
        metrics.put("jobsToday", jobHistoryService.getJobsProcessedToday());
        metrics.put("jobsByType", jobHistoryService.getJobCountsByType());
        
        return metrics;
    }

    // CRUD Operations for Customers
    @PostMapping("/customers")
    public Customer createCustomer(@Valid @RequestBody Customer customer) {
        log.info("Creating new customer: {}", customer.getCustomerName());
        return customerService.saveCustomer(customer);
    }

    @PutMapping("/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        log.info("Updating customer with ID: {}", id);
        customer.setCustomerId(id);
        return customerService.saveCustomer(customer);
    }

    @DeleteMapping("/customers/{id}")
    public Map<String, String> deleteCustomer(@PathVariable Long id) {
        log.info("Deleting customer with ID: {}", id);
        customerService.deleteCustomer(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Customer deleted successfully");
        return response;
    }

    // CRUD Operations for Employees
    @PostMapping("/employees")
    public Employee createEmployee(@Valid @RequestBody Employee employee) {
        log.info("Creating new employee: {}", employee.getFullName());
        return employeeService.saveEmployee(employee);
    }

    @PutMapping("/employees/{id}")
    public Employee updateEmployee(@PathVariable Long id, @Valid @RequestBody Employee employee) {
        log.info("Updating employee with ID: {}", id);
        employee.setEmployeeId(id);
        return employeeService.saveEmployee(employee);
    }

    @DeleteMapping("/employees/{id}")
    public Map<String, String> deleteEmployee(@PathVariable Long id) {
        log.info("Deleting employee with ID: {}", id);
        employeeService.deleteEmployee(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Employee deleted successfully");
        return response;
    }

    // CRUD Operations for Companies
    @PostMapping("/companies")
    public ExternalCompany createCompany(@Valid @RequestBody ExternalCompany company) {
        log.info("Creating new company: {}", company.getCompanyName());
        return companyService.saveCompany(company);
    }

    @PutMapping("/companies/{id}")
    public ExternalCompany updateCompany(@PathVariable Long id, @Valid @RequestBody ExternalCompany company) {
        log.info("Updating company with ID: {}", id);
        company.setCompanyId(id);
        return companyService.saveCompany(company);
    }

    @DeleteMapping("/companies/{id}")
    public Map<String, String> deleteCompany(@PathVariable Long id) {
        log.info("Deleting company with ID: {}", id);
        companyService.deleteCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Company deleted successfully");
        return response;
    }

    // CRUD Operations for Accounts
    @PostMapping("/accounts")
    public Account createAccount(@Valid @RequestBody Account account) {
        log.info("Creating new account: {}", account.getAccountNumber());
        return accountService.saveAccount(account);
    }

    @PutMapping("/accounts/{id}")
    public Account updateAccount(@PathVariable Long id, @Valid @RequestBody Account account) {
        log.info("Updating account with ID: {}", id);
        account.setAccountId(id);
        return accountService.saveAccount(account);
    }

    @DeleteMapping("/accounts/{id}")
    public Map<String, String> deleteAccount(@PathVariable Long id) {
        log.info("Deleting account with ID: {}", id);
        accountService.deleteAccount(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account deleted successfully");
        return response;
    }

    private Map<String, Object> buildCustomerAccountLinkResponse(CustomerAccount link, String action) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", link.getId());
        response.put("accountId", link.getAccount() != null ? link.getAccount().getAccountId() : null);
        response.put("accountNumber", link.getAccount() != null ? link.getAccount().getAccountNumber() : null);
        response.put("customerId", link.getCustomer() != null ? link.getCustomer().getCustomerId() : null);
        response.put("customerName", link.getCustomer() != null ? link.getCustomer().getCustomerName() : null);
        response.put("role", link.getRole() != null ? link.getRole().name() : null);
        response.put("action", action);
        response.put("createdAt", link.getCreatedAt());
        response.put("updatedAt", link.getUpdatedAt());
        return response;
    }

    private Long extractRequiredLong(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fieldName + " must be a valid number");
            }
        }
        throw new IllegalArgumentException(fieldName + " must be a valid number");
    }

    private AccountRole extractRequiredRole(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("role is required (OWNER, ADMIN, NAMED)");
        }
        String roleValue = value.toString().trim();
        if (roleValue.isEmpty()) {
            throw new IllegalArgumentException("role is required (OWNER, ADMIN, NAMED)");
        }
        try {
            return AccountRole.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("role must be one of OWNER, ADMIN, NAMED");
        }
    }
}
