package com.example.camunda.service;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.exception.CustomerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1L);
        testEmployee.setFullName("John Doe");
        testEmployee.setJobTitle("Manager");
        testEmployee.setDepartment("Sales");
        testEmployee.setCreatedAt(LocalDateTime.now());
        testEmployee.setUpdatedAt(LocalDateTime.now());

        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setCustomerName("Test Customer");
        testCustomer.setEmployeeId(1L);
        testCustomer.setCreatedAt(LocalDateTime.now());
        testCustomer.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllCustomers_ShouldReturnAllCustomers() {
        // Arrange
        List<Customer> customers = List.of(testCustomer);
        when(customerRepository.findAll()).thenReturn(customers);

        // Act
        List<Customer> result = customerService.getAllCustomers();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Test Customer");
        verify(customerRepository).findAll();
    }

    @Test
    void findCustomer_WithValidId_ShouldReturnCustomer() {
        // Arrange
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        Optional<Customer> result = customerService.findCustomer(1L, null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCustomerName()).isEqualTo("Test Customer");
        verify(customerRepository).findByCustomerId(1L);
    }

    @Test
    void findCustomer_WithValidName_ShouldReturnCustomer() {
        // Arrange
        when(customerRepository.findByCustomerName("Test Customer")).thenReturn(Optional.of(testCustomer));

        // Act
        Optional<Customer> result = customerService.findCustomer(null, "Test Customer");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCustomerId()).isEqualTo(1L);
        verify(customerRepository).findByCustomerName("Test Customer");
    }

    @Test
    void getCustomerWithEmployee_WithValidData_ShouldReturnCustomer() {
        // Arrange
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = customerService.getCustomerWithEmployee(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomerName()).isEqualTo("Test Customer");
        verify(customerRepository).findByCustomerId(1L);
    }

    @Test
    void getCustomerWithEmployee_WithInvalidData_ShouldThrowException() {
        // Arrange
        when(customerRepository.findByCustomerId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.getCustomerWithEmployee(999L, null))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found with ID: 999");
        
        verify(customerRepository).findByCustomerId(999L);
    }

    @Test
    void saveCustomer_WithValidCustomer_ShouldSaveAndReturn() {
        // Arrange
        when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

        // Act
        Customer result = customerService.saveCustomer(testCustomer);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomerName()).isEqualTo("Test Customer");
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void deleteCustomer_WithValidId_ShouldDeleteCustomer() {
        // Arrange
        when(customerRepository.existsById(1L)).thenReturn(true);

        // Act
        customerService.deleteCustomer(1L);

        // Assert
        verify(customerRepository).existsById(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomer_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(customerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found with ID: 999");
        
        verify(customerRepository).existsById(999L);
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void getEmployeeForCustomer_WithValidCustomer_ShouldReturnEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        Employee result = customerService.getEmployeeForCustomer(testCustomer);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("John Doe");
        verify(employeeRepository).findById(1L);
    }
}
