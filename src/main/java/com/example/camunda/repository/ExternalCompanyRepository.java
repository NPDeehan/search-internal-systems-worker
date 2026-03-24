package com.example.camunda.repository;

import com.example.camunda.model.ExternalCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ExternalCompanyRepository extends JpaRepository<ExternalCompany, Long> {
    Optional<ExternalCompany> findByCompanyId(Long companyId);
    Optional<ExternalCompany> findByCompanyName(String companyName);

    @Query("SELECT COALESCE(MAX(c.companyId), 0) FROM ExternalCompany c")
    Long findMaxCompanyId();
}
