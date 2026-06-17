package com.example.camunda.repository;

import com.example.camunda.model.Package;
import com.example.camunda.model.PackageStatus;
import com.example.camunda.model.ServiceLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, Long>, JpaSpecificationExecutor<Package> {

    Optional<Package> findByTrackingNumber(String trackingNumber);

    List<Package> findByCustomerId(Long customerId);

    List<Package> findByStatus(PackageStatus status);

    List<Package> findByServiceLevel(ServiceLevel serviceLevel);

    @Query("SELECT COALESCE(MAX(p.packageId), 0) FROM Package p")
    Long findMaxPackageId();
}
