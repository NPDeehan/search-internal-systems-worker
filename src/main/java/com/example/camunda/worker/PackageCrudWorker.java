package com.example.camunda.worker;

import com.example.camunda.exception.PackageNotFoundException;
import com.example.camunda.model.Package;
import com.example.camunda.model.PackageStatus;
import com.example.camunda.model.ServiceLevel;
import com.example.camunda.service.PackageService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PackageCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(PackageCrudWorker.class);

    private final PackageService packageService;

    public PackageCrudWorker(PackageService packageService) {
        this.packageService = packageService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-package job: {}", job.getKey());

        Map<String, Object> variables = job.getVariablesAsMap();
        String operation = normalizeOperation(variables.get("operation"));

        if (operation == null) {
            return buildValidationError("Operation must be one of CREATE, UPDATE, DELETE, QUERY", null);
        }

        try {
            return switch (operation) {
                case "CREATE" -> handleCreate(variables);
                case "UPDATE" -> handleUpdate(variables);
                case "DELETE" -> handleDelete(variables);
                case "QUERY"  -> handleQuery(variables);
                default -> buildValidationError("Unsupported operation: " + operation, operation);
            };
        } catch (Exception e) {
            log.error("Error processing package {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    // --- Operation handlers ---

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long packageId            = extractLong(variables.get("packageId"));
        String trackingNumber     = extractString(variables.get("trackingNumber"));
        PackageStatus status      = extractPackageStatus(variables.get("status"));
        ServiceLevel serviceLevel = extractServiceLevel(variables.get("serviceLevel"));
        String senderName         = extractString(variables.get("senderName"));
        String recipientName      = extractString(variables.get("recipientName"));
        String originAddress      = extractString(variables.get("originAddress"));
        String destinationAddress = extractString(variables.get("destinationAddress"));
        BigDecimal weightKg       = extractBigDecimal(variables.get("weightKg"));
        Long customerId           = extractLong(variables.get("customerId"));
        LocalDate shippedDate     = extractLocalDate(variables.get("shippedDate"));
        LocalDate estimatedDeliveryDate = extractLocalDate(variables.get("estimatedDeliveryDate"));
        LocalDate deliveredDate   = extractLocalDate(variables.get("deliveredDate"));
        BigDecimal shippingCost   = extractBigDecimal(variables.get("shippingCost"));
        String contents           = extractString(variables.get("contents"));
        String notes              = extractString(variables.get("notes"));

        String validation = validateCreateInput(serviceLevel, senderName, recipientName,
                originAddress, destinationAddress, weightKg);
        if (validation != null) {
            return buildValidationError(validation, "CREATE");
        }

        if (packageId != null && packageExists(packageId)) {
            return buildValidationError("Package already exists with ID: " + packageId, "CREATE");
        }

        Package pkg = new Package();
        pkg.setPackageId(packageId);
        pkg.setTrackingNumber(trackingNumber);
        pkg.setStatus(status != null ? status : PackageStatus.CREATED);
        pkg.setServiceLevel(serviceLevel);
        pkg.setSenderName(senderName);
        pkg.setRecipientName(recipientName);
        pkg.setOriginAddress(originAddress);
        pkg.setDestinationAddress(destinationAddress);
        pkg.setWeightKg(weightKg);
        pkg.setCustomerId(customerId);
        pkg.setShippedDate(shippedDate);
        pkg.setEstimatedDeliveryDate(estimatedDeliveryDate);
        pkg.setDeliveredDate(deliveredDate);
        pkg.setShippingCost(shippingCost);
        pkg.setContents(contents);
        pkg.setNotes(notes);

        Package saved = packageService.savePackage(pkg);
        return buildSuccessResult("CREATE", saved, "Package created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long packageId = extractLong(variables.get("packageId"));
        if (packageId == null) {
            return buildValidationError("packageId is required for UPDATE operation", "UPDATE");
        }

        Package pkg;
        try {
            pkg = packageService.getPackageById(packageId);
        } catch (PackageNotFoundException ex) {
            return buildNotFoundResult("UPDATE", packageId);
        }

        String trackingNumber     = extractString(variables.get("trackingNumber"));
        PackageStatus status      = extractPackageStatus(variables.get("status"));
        ServiceLevel serviceLevel = extractServiceLevel(variables.get("serviceLevel"));
        String senderName         = extractString(variables.get("senderName"));
        String recipientName      = extractString(variables.get("recipientName"));
        String originAddress      = extractString(variables.get("originAddress"));
        String destinationAddress = extractString(variables.get("destinationAddress"));
        BigDecimal weightKg       = extractBigDecimal(variables.get("weightKg"));
        Long customerId           = extractLong(variables.get("customerId"));
        LocalDate shippedDate     = extractLocalDate(variables.get("shippedDate"));
        LocalDate estimatedDeliveryDate = extractLocalDate(variables.get("estimatedDeliveryDate"));
        LocalDate deliveredDate   = extractLocalDate(variables.get("deliveredDate"));
        BigDecimal shippingCost   = extractBigDecimal(variables.get("shippingCost"));
        String contents           = extractString(variables.get("contents"));
        String notes              = extractString(variables.get("notes"));

        if (trackingNumber     != null) pkg.setTrackingNumber(trackingNumber);
        if (status             != null) pkg.setStatus(status);
        if (serviceLevel       != null) pkg.setServiceLevel(serviceLevel);
        if (senderName         != null) pkg.setSenderName(senderName);
        if (recipientName      != null) pkg.setRecipientName(recipientName);
        if (originAddress      != null) pkg.setOriginAddress(originAddress);
        if (destinationAddress != null) pkg.setDestinationAddress(destinationAddress);
        if (weightKg           != null) pkg.setWeightKg(weightKg);
        if (customerId         != null) pkg.setCustomerId(customerId);
        if (shippedDate        != null) pkg.setShippedDate(shippedDate);
        if (estimatedDeliveryDate != null) pkg.setEstimatedDeliveryDate(estimatedDeliveryDate);
        if (deliveredDate      != null) pkg.setDeliveredDate(deliveredDate);
        if (shippingCost       != null) pkg.setShippingCost(shippingCost);
        if (contents           != null) pkg.setContents(contents);
        if (notes              != null) pkg.setNotes(notes);

        Package saved = packageService.savePackage(pkg);
        return buildSuccessResult("UPDATE", saved, "Package updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long packageId = extractLong(variables.get("packageId"));
        if (packageId == null) {
            return buildValidationError("packageId is required for DELETE operation", "DELETE");
        }

        if (!packageExists(packageId)) {
            return buildNotFoundResult("DELETE", packageId);
        }

        packageService.deletePackage(packageId);

        Map<String, Object> packageCrudResult = new HashMap<>();
        packageCrudResult.put("status", "SUCCESS");
        packageCrudResult.put("operation", "DELETE");
        packageCrudResult.put("message", "Package deleted successfully");
        packageCrudResult.put("packageId", packageId);
        packageCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("packageCrudResult", packageCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("packageId", packageId);

        return result;
    }

    private Map<String, Object> handleQuery(Map<String, Object> variables) {
        Long packageId            = extractLong(variables.get("packageId"));
        Long customerId           = extractLong(variables.get("customerId"));
        PackageStatus status      = extractPackageStatus(variables.get("status"));
        ServiceLevel serviceLevel = extractServiceLevel(variables.get("serviceLevel"));

        List<Package> packages;

        if (packageId != null) {
            // Single-record shortcut — packageId uniquely identifies a package
            try {
                packages = List.of(packageService.getPackageById(packageId));
            } catch (PackageNotFoundException ex) {
                return buildNotFoundResult("QUERY", packageId);
            }
        } else {
            // Combined AND-filter across all provided criteria; empty criteria are ignored
            packages = packageService.searchPackages(customerId, status, serviceLevel);
        }

        List<Map<String, Object>> packageList = new ArrayList<>();
        for (Package p : packages) {
            packageList.add(buildPackageData(p));
        }

        Map<String, Object> packageCrudResult = new HashMap<>();
        packageCrudResult.put("status", "SUCCESS");
        packageCrudResult.put("operation", "QUERY");
        packageCrudResult.put("message", "Query returned " + packageList.size() + " result(s)");
        packageCrudResult.put("packages", packageList);
        packageCrudResult.put("count", packageList.size());
        packageCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("packageCrudResult", packageCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "QUERY");
        result.put("packageCount", packageList.size());

        return result;
    }

    // --- Validation ---

    private String validateCreateInput(ServiceLevel serviceLevel,
                                       String senderName,
                                       String recipientName,
                                       String originAddress,
                                       String destinationAddress,
                                       BigDecimal weightKg) {
        if (serviceLevel       == null) return "serviceLevel is required for CREATE (ECONOMY, STANDARD, EXPRESS, OVERNIGHT)";
        if (senderName         == null) return "senderName is required for CREATE";
        if (recipientName      == null) return "recipientName is required for CREATE";
        if (originAddress      == null) return "originAddress is required for CREATE";
        if (destinationAddress == null) return "destinationAddress is required for CREATE";
        if (weightKg           == null) return "weightKg is required for CREATE";
        return null;
    }

    private boolean packageExists(Long packageId) {
        try {
            packageService.getPackageById(packageId);
            return true;
        } catch (PackageNotFoundException ex) {
            return false;
        }
    }

    // --- Result builders ---

    private Map<String, Object> buildSuccessResult(String operation, Package pkg, String message) {
        Map<String, Object> packageCrudResult = new HashMap<>();
        packageCrudResult.put("status", "SUCCESS");
        packageCrudResult.put("operation", operation);
        packageCrudResult.put("message", message);
        packageCrudResult.put("package", buildPackageData(pkg));
        packageCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("packageCrudResult", packageCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("packageId", pkg.getPackageId());
        result.put("trackingNumber", pkg.getTrackingNumber());

        return result;
    }

    private Map<String, Object> buildPackageData(Package pkg) {
        Map<String, Object> data = new HashMap<>();
        data.put("packageId",             pkg.getPackageId());
        data.put("trackingNumber",        pkg.getTrackingNumber());
        data.put("status",                pkg.getStatus() != null ? pkg.getStatus().name() : null);
        data.put("serviceLevel",          pkg.getServiceLevel() != null ? pkg.getServiceLevel().name() : null);
        data.put("senderName",            pkg.getSenderName());
        data.put("recipientName",         pkg.getRecipientName());
        data.put("originAddress",         pkg.getOriginAddress());
        data.put("destinationAddress",    pkg.getDestinationAddress());
        data.put("weightKg",              pkg.getWeightKg());
        data.put("customerId",            pkg.getCustomerId());
        data.put("shippedDate",           pkg.getShippedDate() != null ? pkg.getShippedDate().toString() : null);
        data.put("estimatedDeliveryDate", pkg.getEstimatedDeliveryDate() != null ? pkg.getEstimatedDeliveryDate().toString() : null);
        data.put("deliveredDate",         pkg.getDeliveredDate() != null ? pkg.getDeliveredDate().toString() : null);
        data.put("shippingCost",          pkg.getShippingCost());
        data.put("contents",              pkg.getContents());
        data.put("notes",                 pkg.getNotes());
        return data;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long packageId) {
        Map<String, Object> packageCrudResult = new HashMap<>();
        packageCrudResult.put("status", "NOT_FOUND");
        packageCrudResult.put("operation", operation);
        packageCrudResult.put("message", "Package not found with ID: " + packageId);
        packageCrudResult.put("packageId", packageId);
        packageCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("packageCrudResult", packageCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("packageId", packageId);
        result.put("trackingNumber", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> packageCrudResult = new HashMap<>();
        packageCrudResult.put("status", "VALIDATION_ERROR");
        packageCrudResult.put("operation", operation);
        packageCrudResult.put("message", message);
        packageCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("packageCrudResult", packageCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("packageId", null);
        result.put("trackingNumber", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> packageCrudResult = new HashMap<>();
        packageCrudResult.put("status", "ERROR");
        packageCrudResult.put("operation", operation);
        packageCrudResult.put("message", "An unexpected error occurred while processing package operation");
        packageCrudResult.put("errorDetails", errorMessage);
        packageCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("packageCrudResult", packageCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("packageId", null);
        result.put("trackingNumber", null);

        return result;
    }

    // --- Extraction helpers ---

    private String normalizeOperation(Object value) {
        String op = extractString(value);
        return op != null ? op.toUpperCase() : null;
    }

    private String extractString(Object value) {
        if (value == null) return null;
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try { return Long.parseLong(trimmed); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private BigDecimal extractBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try { return new BigDecimal(trimmed); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private LocalDate extractLocalDate(Object value) {
        if (value == null) return null;
        String str = extractString(value);
        if (str == null) return null;
        try { return LocalDate.parse(str); } catch (DateTimeParseException e) { return null; }
    }

    private PackageStatus extractPackageStatus(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return PackageStatus.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private ServiceLevel extractServiceLevel(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return ServiceLevel.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }
}
