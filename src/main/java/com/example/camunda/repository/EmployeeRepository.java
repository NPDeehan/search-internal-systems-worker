package com.example.camunda.repository;

import com.example.camunda.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findByFullName(String fullName);
    
    List<Employee> findByFullNameContainingIgnoreCase(String fullName);
    
    List<Employee> findByDepartment(String department);
    
    List<Employee> findByJobTitle(String jobTitle);
    
    @Query("SELECT e FROM Employee e WHERE " +
           "(:fullName IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))) AND " +
           "(:department IS NULL OR LOWER(e.department) = LOWER(:department)) AND " +
           "(:jobTitle IS NULL OR LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :jobTitle, '%')))")
    List<Employee> searchEmployees(@Param("fullName") String fullName, 
                                  @Param("department") String department, 
                                  @Param("jobTitle") String jobTitle);
}
