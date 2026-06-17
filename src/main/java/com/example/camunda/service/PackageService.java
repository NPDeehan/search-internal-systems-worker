package com.example.camunda.service;

import com.example.camunda.exception.PackageNotFoundException;
import com.example.camunda.model.Package;
import com.example.camunda.model.PackageStatus;
import com.example.camunda.model.ServiceLevel;
import com.example.camunda.repository.PackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PackageService {

    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    private final PackageRepository packageRepository;

    public PackageService(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public List<Package> getAllPackages() {
        return packageRepository.findAll();
    }

    public Package getPackageById(Long packageId) {
        return packageRepository.findById(packageId)
                .orElseThrow(() -> new PackageNotFoundException("Package not found with ID: " + packageId));
    }

    public List<Package> getPackagesByCustomerId(Long customerId) {
        return packageRepository.findByCustomerId(customerId);
    }

    public List<Package> getPackagesByStatus(PackageStatus status) {
        return packageRepository.findByStatus(status);
    }

    public List<Package> getPackagesByServiceLevel(ServiceLevel serviceLevel) {
        return packageRepository.findByServiceLevel(serviceLevel);
    }

    public List<Package> searchPackages(Long customerId, PackageStatus status, ServiceLevel serviceLevel) {
        Specification<Package> spec = Specification.where(null);
        if (customerId   != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("customerId"),   customerId));
        if (status       != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"),       status));
        if (serviceLevel != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("serviceLevel"), serviceLevel));
        return packageRepository.findAll(spec);
    }

    @Transactional
    public Package savePackage(Package pkg) {
        if (pkg.getPackageId() == null) {
            Long maxId = packageRepository.findMaxPackageId();
            pkg.setPackageId((maxId == null ? 0L : maxId) + 1L);
        }
        if (pkg.getTrackingNumber() == null || pkg.getTrackingNumber().isBlank()) {
            pkg.setTrackingNumber(String.format("PKG-%06d", pkg.getPackageId()));
        }
        log.debug("Saving package: {}", pkg.getTrackingNumber());
        return packageRepository.save(pkg);
    }

    @Transactional
    public void deletePackage(Long packageId) {
        if (!packageRepository.existsById(packageId)) {
            throw new PackageNotFoundException("Package not found with ID: " + packageId);
        }
        packageRepository.deleteById(packageId);
        log.debug("Deleted package with ID: {}", packageId);
    }
}
