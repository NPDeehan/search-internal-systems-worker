package com.example.camunda.worker;

import com.example.camunda.exception.ProductNotFoundException;
import com.example.camunda.model.Product;
import com.example.camunda.service.ProductService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(ProductCrudWorker.class);

    private final ProductService productService;

    public ProductCrudWorker(ProductService productService) {
        this.productService = productService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-product job: {}", job.getKey());

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
            log.error("Error processing product {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    // --- Operation handlers ---

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long productId       = extractLong(variables.get("productId"));
        String sku           = extractString(variables.get("sku"));
        String name          = extractString(variables.get("name"));
        String description   = extractString(variables.get("description"));
        String category      = extractString(variables.get("category"));
        String manufacturer  = extractString(variables.get("manufacturer"));
        BigDecimal rrp       = extractBigDecimal(variables.get("rrp"));
        BigDecimal unitCost  = extractBigDecimal(variables.get("unitCost"));
        Boolean inStock      = extractBoolean(variables.get("inStock"));
        Integer stockQty     = extractInteger(variables.get("stockQty"));
        String tier          = extractString(variables.get("tier"));

        String validation = validateCreateInput(name, rrp, unitCost);
        if (validation != null) {
            return buildValidationError(validation, "CREATE");
        }

        if (productId != null && productExists(productId)) {
            return buildValidationError("Product already exists with ID: " + productId, "CREATE");
        }

        Product product = new Product();
        product.setProductId(productId);
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setManufacturer(manufacturer);
        product.setRrp(rrp);
        product.setUnitCost(unitCost);
        product.setStockQty(stockQty);
        product.setInStock(inStock);
        product.setTier(tier);

        Product saved = productService.saveProduct(product);
        return buildSuccessResult("CREATE", saved, "Product created successfully");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long productId = extractLong(variables.get("productId"));
        if (productId == null) {
            return buildValidationError("productId is required for UPDATE operation", "UPDATE");
        }

        Product product;
        try {
            product = productService.getProductById(productId);
        } catch (ProductNotFoundException ex) {
            return buildNotFoundResult("UPDATE", productId);
        }

        String sku           = extractString(variables.get("sku"));
        String name          = extractString(variables.get("name"));
        String description   = extractString(variables.get("description"));
        String category      = extractString(variables.get("category"));
        String manufacturer  = extractString(variables.get("manufacturer"));
        BigDecimal rrp       = extractBigDecimal(variables.get("rrp"));
        BigDecimal unitCost  = extractBigDecimal(variables.get("unitCost"));
        Boolean inStock      = extractBoolean(variables.get("inStock"));
        Integer stockQty     = extractInteger(variables.get("stockQty"));
        String tier          = extractString(variables.get("tier"));

        if (sku          != null) product.setSku(sku);
        if (name         != null) product.setName(name);
        if (description   != null) product.setDescription(description);
        if (category     != null) product.setCategory(category);
        if (manufacturer != null) product.setManufacturer(manufacturer);
        if (rrp          != null) product.setRrp(rrp);
        if (unitCost     != null) product.setUnitCost(unitCost);
        if (inStock      != null) product.setInStock(inStock);
        if (stockQty     != null) product.setStockQty(stockQty);
        if (tier         != null) product.setTier(tier);

        Product saved = productService.saveProduct(product);
        return buildSuccessResult("UPDATE", saved, "Product updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long productId = extractLong(variables.get("productId"));
        if (productId == null) {
            return buildValidationError("productId is required for DELETE operation", "DELETE");
        }

        if (!productExists(productId)) {
            return buildNotFoundResult("DELETE", productId);
        }

        productService.deleteProduct(productId);

        Map<String, Object> productCrudResult = new HashMap<>();
        productCrudResult.put("status", "SUCCESS");
        productCrudResult.put("operation", "DELETE");
        productCrudResult.put("message", "Product deleted successfully");
        productCrudResult.put("productId", productId);
        productCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("productCrudResult", productCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("productId", productId);

        return result;
    }

    private Map<String, Object> handleQuery(Map<String, Object> variables) {
        Long productId   = extractLong(variables.get("productId"));
        String name      = extractString(variables.get("name"));
        String category  = extractString(variables.get("category"));
        String tier      = extractString(variables.get("tier"));
        Boolean inStock  = extractBoolean(variables.get("inStock"));

        List<Product> products;

        if (productId != null) {
            // Single-record shortcut — productId uniquely identifies a product
            try {
                products = List.of(productService.getProductById(productId));
            } catch (ProductNotFoundException ex) {
                return buildNotFoundResult("QUERY", productId);
            }
        } else {
            products = productService.searchProducts(name, category, tier, inStock);
        }

        List<Map<String, Object>> productList = new ArrayList<>();
        for (Product p : products) {
            productList.add(buildProductData(p));
        }

        Map<String, Object> productCrudResult = new HashMap<>();
        productCrudResult.put("status", "SUCCESS");
        productCrudResult.put("operation", "QUERY");
        productCrudResult.put("message", "Query returned " + productList.size() + " result(s)");
        productCrudResult.put("products", productList);
        productCrudResult.put("count", productList.size());
        productCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("productCrudResult", productCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "QUERY");
        result.put("productCount", productList.size());

        return result;
    }

    // --- Validation ---

    private String validateCreateInput(String name, BigDecimal rrp, BigDecimal unitCost) {
        if (name     == null) return "name is required for CREATE";
        if (rrp      == null) return "rrp is required for CREATE";
        if (unitCost == null) return "unitCost is required for CREATE";
        return null;
    }

    private boolean productExists(Long productId) {
        try {
            productService.getProductById(productId);
            return true;
        } catch (ProductNotFoundException ex) {
            return false;
        }
    }

    // --- Result builders ---

    private Map<String, Object> buildSuccessResult(String operation, Product product, String message) {
        Map<String, Object> productCrudResult = new HashMap<>();
        productCrudResult.put("status", "SUCCESS");
        productCrudResult.put("operation", operation);
        productCrudResult.put("message", message);
        productCrudResult.put("product", buildProductData(product));
        productCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("productCrudResult", productCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("productId", product.getProductId());
        result.put("sku", product.getSku());

        return result;
    }

    private Map<String, Object> buildProductData(Product product) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId",    product.getProductId());
        data.put("sku",          product.getSku());
        data.put("name",         product.getName());
        data.put("description",  product.getDescription());
        data.put("category",     product.getCategory());
        data.put("manufacturer", product.getManufacturer());
        data.put("rrp",          product.getRrp());
        data.put("unitCost",     product.getUnitCost());
        data.put("margin",       product.getMargin());
        data.put("inStock",      product.getInStock());
        data.put("stockQty",     product.getStockQty());
        data.put("tier",         product.getTier());
        return data;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long productId) {
        Map<String, Object> productCrudResult = new HashMap<>();
        productCrudResult.put("status", "NOT_FOUND");
        productCrudResult.put("operation", operation);
        productCrudResult.put("message", "Product not found with ID: " + productId);
        productCrudResult.put("productId", productId);
        productCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("productCrudResult", productCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("productId", productId);
        result.put("sku", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> productCrudResult = new HashMap<>();
        productCrudResult.put("status", "VALIDATION_ERROR");
        productCrudResult.put("operation", operation);
        productCrudResult.put("message", message);
        productCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("productCrudResult", productCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("productId", null);
        result.put("sku", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> productCrudResult = new HashMap<>();
        productCrudResult.put("status", "ERROR");
        productCrudResult.put("operation", operation);
        productCrudResult.put("message", "An unexpected error occurred while processing product operation");
        productCrudResult.put("errorDetails", errorMessage);
        productCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("productCrudResult", productCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("productId", null);
        result.put("sku", null);

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

    private Integer extractInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return null;
            try { return Integer.parseInt(trimmed); } catch (NumberFormatException e) { return null; }
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

    private Boolean extractBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        String str = value.toString().trim();
        if (str.equalsIgnoreCase("true")) return true;
        if (str.equalsIgnoreCase("false")) return false;
        return null;
    }
}
