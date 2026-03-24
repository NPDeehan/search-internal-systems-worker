package com.example.camunda;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.TrustLevel;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for repository functionality
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "camunda.zeebe.enabled=false",
    "logging.level.com.example.camunda=ERROR"
})
@Transactional
public class RepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    public void testCustomerRepositoryFunctionality() {
        // Test saving a customer
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerName("Test Customer");
        customer.setAddress("1 Test Road");
        customer.setEmail("test.customer@example.test");
        customer.setEmployeeId(1L);
        customer.setTrustLevel(TrustLevel.L1);
        
        Customer savedCustomer = customerRepository.save(customer);
        
        // Verify save worked
        assertNotNull(savedCustomer);
        assertNotNull(savedCustomer.getCustomerId());
        assertEquals("Test Customer", savedCustomer.getCustomerName());
        assertEquals(1L, savedCustomer.getEmployeeId());
        assertEquals(TrustLevel.L1, savedCustomer.getTrustLevel());
        assertEquals("test.customer@example.test", savedCustomer.getEmail());
        
        // Test finding the customer
        assertTrue(customerRepository.findById(savedCustomer.getCustomerId()).isPresent());
        
        // Test count
        long count = customerRepository.count();
        assertTrue(count > 0);
    }

    @Test
    public void testRepositoryIsNotNull() {
        assertNotNull(customerRepository, "Customer repository should be injected");
        assertNotNull(employeeRepository, "Employee repository should be injected");
    }

    @Test
    public void testEmployeeRoleSearch_ShouldMatchJobTitleAndDepartment() {
        Employee salesByTitle = new Employee();
        salesByTitle.setEmployeeId(1001L);
        salesByTitle.setFullName("Sally Title");
        salesByTitle.setJobTitle("Sales Representative");
        salesByTitle.setDepartment("Support");

        Employee salesByDepartment = new Employee();
        salesByDepartment.setEmployeeId(1002L);
        salesByDepartment.setFullName("Derek Department");
        salesByDepartment.setJobTitle("Account Specialist");
        salesByDepartment.setDepartment("Sales");

        Employee nonSales = new Employee();
        nonSales.setEmployeeId(1003L);
        nonSales.setFullName("Ivy Control");
        nonSales.setJobTitle("Risk Analyst");
        nonSales.setDepartment("Compliance");

        employeeRepository.saveAll(List.of(salesByTitle, salesByDepartment, nonSales));

        List<Employee> results = employeeRepository.searchEmployees(null, null, "Sales");

        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(e -> e.getEmployeeId().equals(1001L)));
        assertTrue(results.stream().anyMatch(e -> e.getEmployeeId().equals(1002L)));
        assertFalse(results.stream().anyMatch(e -> e.getEmployeeId().equals(1003L)));
    }
}
