package com.example.camunda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_policies")
@EntityListeners(AuditingEntityListener.class)
public class InsurancePolicy {

    @Id
    @Positive(message = "Policy ID must be positive")
    private Long policyId;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Policy type is required")
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Policy status is required")
    private PolicyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Holder type is required")
    private PolicyHolderType holderType;

    // Set when holderType = CUSTOMER
    private Long customerId;

    // Set when holderType = EXTERNAL_COMPANY
    private Long companyId;

    @Column(nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Premium amount is required")
    private BigDecimal premiumAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Coverage amount is required")
    private BigDecimal coverageAmount;

    @Column(length = 1000)
    private String notes;

    // --- CAR policy fields ---
    private String vehicleRegistration;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;

    // --- HOME policy fields ---
    private String propertyAddress;
    private String propertyType;

    @Column(precision = 14, scale = 2)
    private BigDecimal propertyValue;

    // --- PET policy fields ---
    private String petName;
    private String petSpecies;
    private String petBreed;
    private Integer petAge;

    // --- LONG_TERM_INJURY policy fields ---
    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyBenefit;
    private Integer waitingPeriodDays;
    private Integer maxBenefitPeriodMonths;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // --- Getters and Setters ---

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public PolicyType getPolicyType() { return policyType; }
    public void setPolicyType(PolicyType policyType) { this.policyType = policyType; }

    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }

    public PolicyHolderType getHolderType() { return holderType; }
    public void setHolderType(PolicyHolderType holderType) { this.holderType = holderType; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }

    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getVehicleRegistration() { return vehicleRegistration; }
    public void setVehicleRegistration(String vehicleRegistration) { this.vehicleRegistration = vehicleRegistration; }

    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public BigDecimal getPropertyValue() { return propertyValue; }
    public void setPropertyValue(BigDecimal propertyValue) { this.propertyValue = propertyValue; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetSpecies() { return petSpecies; }
    public void setPetSpecies(String petSpecies) { this.petSpecies = petSpecies; }

    public String getPetBreed() { return petBreed; }
    public void setPetBreed(String petBreed) { this.petBreed = petBreed; }

    public Integer getPetAge() { return petAge; }
    public void setPetAge(Integer petAge) { this.petAge = petAge; }

    public BigDecimal getMonthlyBenefit() { return monthlyBenefit; }
    public void setMonthlyBenefit(BigDecimal monthlyBenefit) { this.monthlyBenefit = monthlyBenefit; }

    public Integer getWaitingPeriodDays() { return waitingPeriodDays; }
    public void setWaitingPeriodDays(Integer waitingPeriodDays) { this.waitingPeriodDays = waitingPeriodDays; }

    public Integer getMaxBenefitPeriodMonths() { return maxBenefitPeriodMonths; }
    public void setMaxBenefitPeriodMonths(Integer maxBenefitPeriodMonths) { this.maxBenefitPeriodMonths = maxBenefitPeriodMonths; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
