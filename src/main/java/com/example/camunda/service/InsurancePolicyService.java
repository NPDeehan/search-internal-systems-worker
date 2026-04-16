package com.example.camunda.service;

import com.example.camunda.exception.InsurancePolicyNotFoundException;
import com.example.camunda.model.InsurancePolicy;
import com.example.camunda.model.PolicyHolderType;
import com.example.camunda.model.PolicyStatus;
import com.example.camunda.model.PolicyType;
import com.example.camunda.repository.InsurancePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InsurancePolicyService {

    private static final Logger log = LoggerFactory.getLogger(InsurancePolicyService.class);

    private final InsurancePolicyRepository policyRepository;

    public InsurancePolicyService(InsurancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public List<InsurancePolicy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public InsurancePolicy getPolicyById(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new InsurancePolicyNotFoundException("Insurance policy not found with ID: " + policyId));
    }

    public List<InsurancePolicy> getPoliciesByCustomerId(Long customerId) {
        return policyRepository.findByCustomerId(customerId);
    }

    public List<InsurancePolicy> getPoliciesByCompanyId(Long companyId) {
        return policyRepository.findByCompanyId(companyId);
    }

    public List<InsurancePolicy> getPoliciesByType(PolicyType policyType) {
        return policyRepository.findByPolicyType(policyType);
    }

    public List<InsurancePolicy> getPoliciesByStatus(PolicyStatus status) {
        return policyRepository.findByStatus(status);
    }

    public List<InsurancePolicy> getPoliciesByHolderType(PolicyHolderType holderType) {
        return policyRepository.findByHolderType(holderType);
    }

    public List<InsurancePolicy> searchPolicies(Long customerId, Long companyId,
                                                PolicyType policyType, PolicyStatus status,
                                                PolicyHolderType holderType) {
        Specification<InsurancePolicy> spec = Specification.where(null);
        if (customerId  != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("customerId"),  customerId));
        if (companyId   != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("companyId"),   companyId));
        if (policyType  != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("policyType"),  policyType));
        if (status      != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"),      status));
        if (holderType  != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("holderType"),  holderType));
        return policyRepository.findAll(spec);
    }

    @Transactional
    public InsurancePolicy savePolicy(InsurancePolicy policy) {
        if (policy.getPolicyId() == null) {
            Long maxId = policyRepository.findMaxPolicyId();
            policy.setPolicyId((maxId == null ? 0L : maxId) + 1L);
        }
        if (policy.getPolicyNumber() == null || policy.getPolicyNumber().isBlank()) {
            policy.setPolicyNumber(String.format("POL-%06d", policy.getPolicyId()));
        }
        log.debug("Saving insurance policy: {}", policy.getPolicyNumber());
        return policyRepository.save(policy);
    }

    @Transactional
    public void deletePolicy(Long policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new InsurancePolicyNotFoundException("Insurance policy not found with ID: " + policyId);
        }
        policyRepository.deleteById(policyId);
        log.debug("Deleted insurance policy with ID: {}", policyId);
    }
}
