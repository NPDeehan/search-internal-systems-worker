package com.example.camunda.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
public class Customer {
    @Id
    @Positive(message = "Customer ID must be positive")
    private Long customerId;
    
    @Column(nullable = false)
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Column(nullable = false)
    @NotBlank(message = "Customer address is required")
    private String address;

    @Column(unique = true)
    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be a valid email address")
    private String email;
    
    @Column(nullable = false)
    @NotNull(message = "Employee ID is required")
    @Positive(message = "Employee ID must be positive")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    @NotNull(message = "Trust level is required")
    private TrustLevel trustLevel;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Lazy-loaded relationship to Employee
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employeeId", insertable = false, updatable = false)
    @JsonBackReference
    private Employee employee;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<CustomerAccount> customerAccounts;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public List<CustomerAccount> getCustomerAccounts() {
        return customerAccounts;
    }

    public void setCustomerAccounts(List<CustomerAccount> customerAccounts) {
        this.customerAccounts = customerAccounts;
    }
}
