package com.example.camunda.repository;

import com.example.camunda.model.InsurancePolicy;
import com.example.camunda.model.PolicyHolderType;
import com.example.camunda.model.PolicyStatus;
import com.example.camunda.model.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long>, JpaSpecificationExecutor<InsurancePolicy> {

    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);

    List<InsurancePolicy> findByCustomerId(Long customerId);

    List<InsurancePolicy> findByCompanyId(Long companyId);

    List<InsurancePolicy> findByPolicyType(PolicyType policyType);

    List<InsurancePolicy> findByStatus(PolicyStatus status);

    List<InsurancePolicy> findByHolderType(PolicyHolderType holderType);

    @Query("SELECT COALESCE(MAX(p.policyId), 0) FROM InsurancePolicy p")
    Long findMaxPolicyId();
}
