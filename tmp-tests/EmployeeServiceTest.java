package com.example.camunda.service;

import com.example.camunda.model.Employee;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.exception.EmployeeNotFoundException;
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
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1L);
        testEmployee.setFullName("John Doe");
        testEmployee.setJobTitle("Software Engineer");
        testEmployee.setDepartment("IT");
        testEmployee.setPhoneNumber("555-1234");
        testEmployee.setCreatedAt(LocalDateTime.now());
        testEmployee.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllEmployees_ShouldReturnAllEmployees() {
        // Arrange
        List<Employee> employees = List.of(testEmployee);
        when(employeeRepository.findAll()).thenReturn(employees);

        // Act
        List<Employee> result = employeeService.getAllEmployees();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("John Doe");
        verify(employeeRepository).findAll();
    }

    @Test
    void getEmployeeById_WithValidId_ShouldReturnEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        Employee result = employeeService.getEmployeeById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getJobTitle()).isEqualTo("Software Engineer");
        verify(employeeRepository).findById(1L);
    }

    @Test
    void getEmployeeById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeById(999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("Employee not found with ID: 999");
        
        verify(employeeRepository).findById(999L);
    }

    @Test
    void saveEmployee_WithValidEmployee_ShouldSaveAndReturn() {
        // Arrange
        when(employeeRepository.save(testEmployee)).thenReturn(testEmployee);

        // Act
        Employee result = employeeService.saveEmployee(testEmployee);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getDepartment()).isEqualTo("IT");
        verify(employeeRepository).save(testEmployee);
    }

    @Test
    void deleteEmployee_WithValidId_ShouldDeleteEmployee() {
        // Arrange
        when(employeeRepository.existsById(1L)).thenReturn(true);

        // Act
        employeeService.deleteEmployee(1L);

        // Assert
        verify(employeeRepository).existsById(1L);
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void deleteEmployee_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(employeeRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.deleteEmployee(999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("Employee not found with ID: 999");
        
        verify(employeeRepository).existsById(999L);
        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    void getEmployeesByDepartment_ShouldReturnFilteredEmployees() {
        // Arrange
        List<Employee> employees = List.of(testEmployee);
        when(employeeRepository.findByDepartment("IT")).thenReturn(employees);

        // Act
        List<Employee> result = employeeService.getEmployeesByDepartment("IT");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo("IT");
        verify(employeeRepository).findByDepartment("IT");
    }
}
