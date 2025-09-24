package com.example.camunda;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.repository.ExternalCompanyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initData(CustomerRepository customerRepo, EmployeeRepository employeeRepo, ExternalCompanyRepository companyRepo) {
        return args -> {
            // Employees
            Employee emp1 = new Employee();
            emp1.setEmployeeId(1L);
            emp1.setFullName("Alice Smith");
            emp1.setJobTitle("Account Manager");
            emp1.setDepartment("Sales");
            emp1.setPhoneNumber("123-456-7890");
            employeeRepo.save(emp1);

            Employee emp2 = new Employee();
            emp2.setEmployeeId(2L);
            emp2.setFullName("Bob Johnson");
            emp2.setJobTitle("Support Lead");
            emp2.setDepartment("Support");
            emp2.setPhoneNumber("987-654-3210");
            employeeRepo.save(emp2);

            // Customers
            Customer cust1 = new Customer();
            cust1.setCustomerId(100L);
            cust1.setCustomerName("Acme Corp");
            cust1.setEmployeeId(1L);
            customerRepo.save(cust1);

            Customer cust2 = new Customer();
            cust2.setCustomerId(200L);
            cust2.setCustomerName("Beta LLC");
            cust2.setEmployeeId(2L);
            customerRepo.save(cust2);

            // External Companies
            ExternalCompany comp1 = new ExternalCompany();
            comp1.setCompanyId(1000L);
            comp1.setCompanyName("Globex Inc");
            comp1.setAddress("1 Main St, Metropolis");
            comp1.setContactPerson("Jane Doe");
            comp1.setPhoneNumber("555-111-2222");
            companyRepo.save(comp1);

            ExternalCompany comp2 = new ExternalCompany();
            comp2.setCompanyId(2000L);
            comp2.setCompanyName("Initech");
            comp2.setAddress("42 Silicon Ave, Tech City");
            comp2.setContactPerson("John Roe");
            comp2.setPhoneNumber("555-333-4444");
            companyRepo.save(comp2);
        };
    }
}
