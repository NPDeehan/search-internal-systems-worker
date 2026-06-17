package com.example.camunda.worker;

import com.example.camunda.exception.ProductNotFoundException;
import com.example.camunda.exception.PurchaseItemNotFoundException;
import com.example.camunda.exception.PurchaseOrderNotFoundException;
import com.example.camunda.model.PurchaseItem;
import com.example.camunda.service.PurchaseItemService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flat, no-FEEL connector for managing individual purchase items against an existing order.
 * One call = one item. The parent order's total is recomputed automatically by the service.
 */
@Component
public class PurchaseItemCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(PurchaseItemCrudWorker.class);

    private final PurchaseItemService itemService;

    public PurchaseItemCrudWorker(PurchaseItemService itemService) {
        this.itemService = itemService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-purchase-item job: {}", job.getKey());

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
            log.error("Error processing purchase item {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    // --- Operation handlers ---

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long orderId         = extractLong(variables.get("orderId"));
        Long productId       = extractLong(variables.get("productId"));
        Integer quantity     = extractInteger(variables.get("quantity"));
        BigDecimal unitPrice = extractBigDecimal(variables.get("unitPrice"));

        if (orderId == null)   return buildValidationError("orderId is required for CREATE", "CREATE");
        if (productId == null) return buildValidationError("productId is required for CREATE", "CREATE");

        try {
            PurchaseItem item = itemService.addItem(orderId, productId, quantity, unitPrice);
            return buildSuccessResult("CREATE", item, orderId, "Purchase item added successfully");
        } catch (PurchaseOrderNotFoundException ex) {
            return buildValidationError("orderId references unknown order " + orderId, "CREATE");
        } catch (ProductNotFoundException ex) {
            return buildValidationError("productId references unknown product " + productId, "CREATE");
        }
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long purchaseItemId  = extractLong(variables.get("purchaseItemId"));
        if (purchaseItemId == null) {
            return buildValidationError("purchaseItemId is required for UPDATE operation", "UPDATE");
        }
        Long productId       = extractLong(variables.get("productId"));
        Integer quantity     = extractInteger(variables.get("quantity"));
        BigDecimal unitPrice = extractBigDecimal(variables.get("unitPrice"));

        try {
            PurchaseItem item = itemService.updateItem(purchaseItemId, productId, quantity, unitPrice);
            return buildSuccessResult("UPDATE", item, item.getOrderId(), "Purchase item updated successfully");
        } catch (PurchaseItemNotFoundException ex) {
            return buildNotFoundResult("UPDATE", purchaseItemId);
        } catch (ProductNotFoundException ex) {
            return buildValidationError("productId references unknown product " + productId, "UPDATE");
        }
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long purchaseItemId = extractLong(variables.get("purchaseItemId"));
        if (purchaseItemId == null) {
            return buildValidationError("purchaseItemId is required for DELETE operation", "DELETE");
        }

        try {
            itemService.deleteItem(purchaseItemId);
        } catch (PurchaseItemNotFoundException ex) {
            return buildNotFoundResult("DELETE", purchaseItemId);
        }

        Map<String, Object> itemCrudResult = new HashMap<>();
        itemCrudResult.put("status", "SUCCESS");
        itemCrudResult.put("operation", "DELETE");
        itemCrudResult.put("message", "Purchase item deleted successfully");
        itemCrudResult.put("purchaseItemId", purchaseItemId);
        itemCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("itemCrudResult", itemCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("purchaseItemId", purchaseItemId);

        return result;
    }

    private Map<String, Object> handleQuery(Map<String, Object> variables) {
        Long purchaseItemId = extractLong(variables.get("purchaseItemId"));
        Long orderId        = extractLong(variables.get("orderId"));

        List<Map<String, Object>> itemList = new ArrayList<>();

        if (purchaseItemId != null) {
            try {
                itemList.add(buildItemData(itemService.getItemById(purchaseItemId), null));
            } catch (PurchaseItemNotFoundException ex) {
                return buildNotFoundResult("QUERY", purchaseItemId);
            }
        } else if (orderId != null) {
            try {
                for (PurchaseItem item : itemService.getItemsByOrder(orderId)) {
                    itemList.add(buildItemData(item, orderId));
                }
            } catch (PurchaseOrderNotFoundException ex) {
                return buildValidationError("orderId references unknown order " + orderId, "QUERY");
            }
        } else {
            return buildValidationError("QUERY requires either purchaseItemId or orderId", "QUERY");
        }

        Map<String, Object> itemCrudResult = new HashMap<>();
        itemCrudResult.put("status", "SUCCESS");
        itemCrudResult.put("operation", "QUERY");
        itemCrudResult.put("message", "Query returned " + itemList.size() + " result(s)");
        itemCrudResult.put("items", itemList);
        itemCrudResult.put("count", itemList.size());
        itemCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("itemCrudResult", itemCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "QUERY");
        result.put("itemCount", itemList.size());

        return result;
    }

    // --- Result builders ---

    private Map<String, Object> buildSuccessResult(String operation, PurchaseItem item, Long orderId, String message) {
        Map<String, Object> itemCrudResult = new HashMap<>();
        itemCrudResult.put("status", "SUCCESS");
        itemCrudResult.put("operation", operation);
        itemCrudResult.put("message", message);
        itemCrudResult.put("item", buildItemData(item, orderId));
        itemCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("itemCrudResult", itemCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("purchaseItemId", item.getPurchaseItemId());
        result.put("orderId", item.getOrderId() != null ? item.getOrderId() : orderId);

        return result;
    }

    private Map<String, Object> buildItemData(PurchaseItem item, Long fallbackOrderId) {
        Map<String, Object> data = new HashMap<>();
        data.put("purchaseItemId", item.getPurchaseItemId());
        data.put("orderId",        item.getOrderId() != null ? item.getOrderId() : fallbackOrderId);
        data.put("productId",      item.getProductId());
        data.put("productName",    item.getProductName());
        data.put("quantity",       item.getQuantity());
        data.put("unitPrice",      item.getUnitPrice());
        data.put("lineTotal",      item.getLineTotal());
        return data;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long purchaseItemId) {
        Map<String, Object> itemCrudResult = new HashMap<>();
        itemCrudResult.put("status", "NOT_FOUND");
        itemCrudResult.put("operation", operation);
        itemCrudResult.put("message", "Purchase item not found with ID: " + purchaseItemId);
        itemCrudResult.put("purchaseItemId", purchaseItemId);
        itemCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("itemCrudResult", itemCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("purchaseItemId", purchaseItemId);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> itemCrudResult = new HashMap<>();
        itemCrudResult.put("status", "VALIDATION_ERROR");
        itemCrudResult.put("operation", operation);
        itemCrudResult.put("message", message);
        itemCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("itemCrudResult", itemCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("purchaseItemId", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> itemCrudResult = new HashMap<>();
        itemCrudResult.put("status", "ERROR");
        itemCrudResult.put("operation", operation);
        itemCrudResult.put("message", "An unexpected error occurred while processing purchase item operation");
        itemCrudResult.put("errorDetails", errorMessage);
        itemCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("itemCrudResult", itemCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("purchaseItemId", null);

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
}
