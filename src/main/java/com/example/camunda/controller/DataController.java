package com.example.camunda.controller;

import com.example.camunda.dto.JobHistoryDTO;
import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.model.JobHistory;
import com.example.camunda.service.CustomerService;
import com.example.camunda.service.EmployeeService;
import com.example.camunda.service.CompanyService;
import com.example.camunda.service.JobHistoryService;
import com.example.camunda.service.ZeebeConnectionService;
import com.example.camunda.ZeebeJobPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DataController {
    
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final CompanyService companyService;
    private final JobHistoryService jobHistoryService;
    private final ZeebeConnectionService zeebeConnectionService;
    private final ZeebeJobPollingService pollingService;

    @GetMapping("/customers")
    public List<Customer> getCustomers() {
        log.debug("Fetching all customers");
        return customerService.getAllCustomers();
    }

    @GetMapping("/employees")
    public List<Employee> getEmployees() {
        log.debug("Fetching all employees");
        return employeeService.getAllEmployees();
    }

    @GetMapping("/companies")
    public List<ExternalCompany> getCompanies() {
        log.debug("Fetching all companies");
        return companyService.getAllCompanies();
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
        
        return status;
    }

    @GetMapping("/job-history")
    public List<JobHistoryDTO> getJobHistory() {
        log.debug("Fetching enhanced job history");
        List<JobHistory> jobHistories = jobHistoryService.getRecentJobHistory(100);
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
}
