package com.example.camunda.repository;

import com.example.camunda.model.ExternalCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExternalCompanyRepository extends JpaRepository<ExternalCompany, Long> {
    Optional<ExternalCompany> findByCompanyId(Long companyId);
    Optional<ExternalCompany> findByCompanyName(String companyName);
}
