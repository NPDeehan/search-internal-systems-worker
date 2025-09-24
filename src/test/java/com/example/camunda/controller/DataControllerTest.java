package com.example.camunda.controller;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.service.CustomerService;
import com.example.camunda.service.EmployeeService;
import com.example.camunda.service.CompanyService;
import com.example.camunda.service.JobHistoryService;
import com.example.camunda.service.ZeebeConnectionService;
import com.example.camunda.ZeebeJobPollingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataController.class)
@ActiveProfiles("test")
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private JobHistoryService jobHistoryService;

    @MockBean
    private ZeebeConnectionService zeebeConnectionService;

    @MockBean
    private ZeebeJobPollingService pollingService;

    private Customer testCustomer;
    private Employee testEmployee;
    private ExternalCompany testCompany;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1L);
        testEmployee.setFullName("John Doe");
        testEmployee.setJobTitle("Software Engineer");
        testEmployee.setDepartment("IT");
        testEmployee.setCreatedAt(LocalDateTime.now());
        testEmployee.setUpdatedAt(LocalDateTime.now());

        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setCustomerName("Test Customer");
        testCustomer.setEmployeeId(1L);
        testCustomer.setCreatedAt(LocalDateTime.now());
        testCustomer.setUpdatedAt(LocalDateTime.now());

        testCompany = new ExternalCompany();
        testCompany.setCompanyId(1L);
        testCompany.setCompanyName("Test Company");
        testCompany.setAddress("123 Test St");
        testCompany.setContactPerson("Jane Smith");
        testCompany.setCreatedAt(LocalDateTime.now());
        testCompany.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getCustomers_ShouldReturnCustomersList() throws Exception {
        // Arrange
        when(customerService.getAllCustomers()).thenReturn(List.of(testCustomer));

        // Act & Assert
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Test Customer"));

        verify(customerService).getAllCustomers();
    }

    @Test
    void getEmployees_ShouldReturnEmployeesList() throws Exception {
        // Arrange
        when(employeeService.getAllEmployees()).thenReturn(List.of(testEmployee));

        // Act & Assert
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employeeId").value(1))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));

        verify(employeeService).getAllEmployees();
    }

    @Test
    void getCompanies_ShouldReturnCompaniesList() throws Exception {
        // Arrange
        when(companyService.getAllCompanies()).thenReturn(List.of(testCompany));

        // Act & Assert
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].companyId").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Test Company"));

        verify(companyService).getAllCompanies();
    }

    @Test
    void getConnectionStatus_WhenConnected_ShouldReturnConnectedStatus() throws Exception {
        // Arrange
        when(zeebeConnectionService.isConnected()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/connection-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.connected").value(true));

        verify(zeebeConnectionService).isConnected();
    }

    @Test
    void getConnectionStatus_WhenDisconnected_ShouldReturnDisconnectedStatusWithError() throws Exception {
        // Arrange
        when(zeebeConnectionService.isConnected()).thenReturn(false);
        when(zeebeConnectionService.getLastError()).thenReturn("Connection timeout");

        // Act & Assert
        mockMvc.perform(get("/api/connection-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.error").value("Connection timeout"));

        verify(zeebeConnectionService).isConnected();
        verify(zeebeConnectionService).getLastError();
    }

    @Test
    void getWorkerStatus_ShouldReturnWorkerStatus() throws Exception {
        // Arrange
        when(pollingService.isPollingActive()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/worker-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.['match-customer-with-dri']").value("RUNNING"))
                .andExpect(jsonPath("$.['query-for-company']").value("RUNNING"));

        verify(pollingService).isPollingActive();
    }

    @Test
    void createCustomer_WithValidData_ShouldCreateCustomer() throws Exception {
        // Arrange
        when(customerService.saveCustomer(any(Customer.class))).thenReturn(testCustomer);

        // Act & Assert
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCustomer)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Test Customer"));

        verify(customerService).saveCustomer(any(Customer.class));
    }

    @Test
    void createEmployee_WithValidData_ShouldCreateEmployee() throws Exception {
        // Arrange
        when(employeeService.saveEmployee(any(Employee.class))).thenReturn(testEmployee);

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmployee)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(employeeService).saveEmployee(any(Employee.class));
    }

    @Test
    void updateCustomer_WithValidData_ShouldUpdateCustomer() throws Exception {
        // Arrange
        when(customerService.saveCustomer(any(Customer.class))).thenReturn(testCustomer);

        // Act & Assert
        mockMvc.perform(put("/api/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCustomer)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Test Customer"));

        verify(customerService).saveCustomer(any(Customer.class));
    }

    @Test
    void deleteCustomer_WithValidId_ShouldDeleteCustomer() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Customer deleted successfully"));

        verify(customerService).deleteCustomer(1L);
    }
}
