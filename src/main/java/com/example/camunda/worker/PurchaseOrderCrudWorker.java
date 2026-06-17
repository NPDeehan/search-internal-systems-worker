package com.example.camunda.worker;

import com.example.camunda.exception.PurchaseOrderNotFoundException;
import com.example.camunda.model.OrderStatus;
import com.example.camunda.model.PurchaseItem;
import com.example.camunda.model.PurchaseOrder;
import com.example.camunda.service.PurchaseOrderService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PurchaseOrderCrudWorker {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderCrudWorker.class);

    private final PurchaseOrderService orderService;

    public PurchaseOrderCrudWorker(PurchaseOrderService orderService) {
        this.orderService = orderService;
    }

    public Map<String, Object> handleJob(final ActivatedJob job) {
        log.debug("Processing manage-purchase-order job: {}", job.getKey());

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
            log.error("Error processing purchase order {} operation: {}", operation, e.getMessage(), e);
            return buildErrorResult(operation, e.getMessage());
        }
    }

    // --- Operation handlers ---

    private Map<String, Object> handleCreate(Map<String, Object> variables) {
        Long orderId            = extractLong(variables.get("orderId"));
        String orderNumber      = extractString(variables.get("orderNumber"));
        OrderStatus status      = extractOrderStatus(variables.get("status"));
        Long customerId         = extractLong(variables.get("customerId"));
        LocalDate orderDate     = extractLocalDate(variables.get("orderDate"));
        LocalDate expectedDate  = extractLocalDate(variables.get("expectedDeliveryDate"));
        String notes            = extractString(variables.get("notes"));

        if (orderDate == null) {
            return buildValidationError("orderDate is required for CREATE (YYYY-MM-DD)", "CREATE");
        }
        if (orderId != null && orderExists(orderId)) {
            return buildValidationError("Purchase order already exists with ID: " + orderId, "CREATE");
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(orderId);
        order.setOrderNumber(orderNumber);
        order.setStatus(status != null ? status : OrderStatus.DRAFT);
        order.setCustomerId(customerId);
        order.setOrderDate(orderDate);
        order.setExpectedDeliveryDate(expectedDate);
        order.setNotes(notes);

        PurchaseOrder saved = orderService.saveOrder(order);
        return buildSuccessResult("CREATE", saved, "Purchase order created successfully. Add items with manage-purchase-item.");
    }

    private Map<String, Object> handleUpdate(Map<String, Object> variables) {
        Long orderId = extractLong(variables.get("orderId"));
        if (orderId == null) {
            return buildValidationError("orderId is required for UPDATE operation", "UPDATE");
        }

        PurchaseOrder order;
        try {
            order = orderService.getOrderById(orderId);
        } catch (PurchaseOrderNotFoundException ex) {
            return buildNotFoundResult("UPDATE", orderId);
        }

        String orderNumber      = extractString(variables.get("orderNumber"));
        OrderStatus status      = extractOrderStatus(variables.get("status"));
        Long customerId         = extractLong(variables.get("customerId"));
        LocalDate orderDate     = extractLocalDate(variables.get("orderDate"));
        LocalDate expectedDate  = extractLocalDate(variables.get("expectedDeliveryDate"));
        String notes            = extractString(variables.get("notes"));

        if (orderNumber != null) order.setOrderNumber(orderNumber);
        if (status      != null) order.setStatus(status);
        if (customerId  != null) order.setCustomerId(customerId);
        if (orderDate   != null) order.setOrderDate(orderDate);
        if (expectedDate != null) order.setExpectedDeliveryDate(expectedDate);
        if (notes       != null) order.setNotes(notes);

        PurchaseOrder saved = orderService.saveOrder(order);
        return buildSuccessResult("UPDATE", saved, "Purchase order updated successfully");
    }

    private Map<String, Object> handleDelete(Map<String, Object> variables) {
        Long orderId = extractLong(variables.get("orderId"));
        if (orderId == null) {
            return buildValidationError("orderId is required for DELETE operation", "DELETE");
        }

        if (!orderExists(orderId)) {
            return buildNotFoundResult("DELETE", orderId);
        }

        orderService.deleteOrder(orderId);

        Map<String, Object> orderCrudResult = new HashMap<>();
        orderCrudResult.put("status", "SUCCESS");
        orderCrudResult.put("operation", "DELETE");
        orderCrudResult.put("message", "Purchase order deleted successfully");
        orderCrudResult.put("orderId", orderId);
        orderCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("orderCrudResult", orderCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "DELETE");
        result.put("orderId", orderId);

        return result;
    }

    private Map<String, Object> handleQuery(Map<String, Object> variables) {
        Long orderId       = extractLong(variables.get("orderId"));
        Long customerId    = extractLong(variables.get("customerId"));
        OrderStatus status = extractOrderStatus(variables.get("status"));

        List<PurchaseOrder> orders;

        if (orderId != null) {
            // Single-record shortcut — orderId uniquely identifies an order
            try {
                orders = List.of(orderService.getOrderById(orderId));
            } catch (PurchaseOrderNotFoundException ex) {
                return buildNotFoundResult("QUERY", orderId);
            }
        } else {
            orders = orderService.searchOrders(customerId, status);
        }

        List<Map<String, Object>> orderList = new ArrayList<>();
        for (PurchaseOrder o : orders) {
            orderList.add(buildOrderData(o));
        }

        Map<String, Object> orderCrudResult = new HashMap<>();
        orderCrudResult.put("status", "SUCCESS");
        orderCrudResult.put("operation", "QUERY");
        orderCrudResult.put("message", "Query returned " + orderList.size() + " result(s)");
        orderCrudResult.put("orders", orderList);
        orderCrudResult.put("count", orderList.size());
        orderCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("orderCrudResult", orderCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", "QUERY");
        result.put("orderCount", orderList.size());

        return result;
    }

    private boolean orderExists(Long orderId) {
        try {
            orderService.getOrderById(orderId);
            return true;
        } catch (PurchaseOrderNotFoundException ex) {
            return false;
        }
    }

    // --- Result builders ---

    private Map<String, Object> buildSuccessResult(String operation, PurchaseOrder order, String message) {
        Map<String, Object> orderCrudResult = new HashMap<>();
        orderCrudResult.put("status", "SUCCESS");
        orderCrudResult.put("operation", operation);
        orderCrudResult.put("message", message);
        orderCrudResult.put("order", buildOrderData(order));
        orderCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("orderCrudResult", orderCrudResult);
        result.put("operationStatus", "SUCCESS");
        result.put("operation", operation);
        result.put("orderId", order.getOrderId());
        result.put("orderNumber", order.getOrderNumber());

        return result;
    }

    private Map<String, Object> buildOrderData(PurchaseOrder order) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId",              order.getOrderId());
        data.put("orderNumber",          order.getOrderNumber());
        data.put("status",               order.getStatus() != null ? order.getStatus().name() : null);
        data.put("customerId",           order.getCustomerId());
        data.put("orderDate",            order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        data.put("expectedDeliveryDate", order.getExpectedDeliveryDate() != null ? order.getExpectedDeliveryDate().toString() : null);
        data.put("totalAmount",          order.getTotalAmount());
        data.put("notes",                order.getNotes());
        data.put("itemCount",            order.getItemCount());

        List<Map<String, Object>> itemData = new ArrayList<>();
        if (order.getItems() != null) {
            for (PurchaseItem item : order.getItems()) {
                Map<String, Object> id = new HashMap<>();
                id.put("purchaseItemId", item.getPurchaseItemId());
                id.put("productId",      item.getProductId());
                id.put("productName",    item.getProductName());
                id.put("quantity",       item.getQuantity());
                id.put("unitPrice",      item.getUnitPrice());
                id.put("lineTotal",      item.getLineTotal());
                itemData.add(id);
            }
        }
        data.put("items", itemData);
        return data;
    }

    private Map<String, Object> buildNotFoundResult(String operation, Long orderId) {
        Map<String, Object> orderCrudResult = new HashMap<>();
        orderCrudResult.put("status", "NOT_FOUND");
        orderCrudResult.put("operation", operation);
        orderCrudResult.put("message", "Purchase order not found with ID: " + orderId);
        orderCrudResult.put("orderId", orderId);
        orderCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("orderCrudResult", orderCrudResult);
        result.put("operationStatus", "NOT_FOUND");
        result.put("operation", operation);
        result.put("orderId", orderId);
        result.put("orderNumber", null);

        return result;
    }

    private Map<String, Object> buildValidationError(String message, String operation) {
        Map<String, Object> orderCrudResult = new HashMap<>();
        orderCrudResult.put("status", "VALIDATION_ERROR");
        orderCrudResult.put("operation", operation);
        orderCrudResult.put("message", message);
        orderCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("orderCrudResult", orderCrudResult);
        result.put("operationStatus", "VALIDATION_ERROR");
        result.put("operation", operation);
        result.put("orderId", null);
        result.put("orderNumber", null);

        return result;
    }

    private Map<String, Object> buildErrorResult(String operation, String errorMessage) {
        Map<String, Object> orderCrudResult = new HashMap<>();
        orderCrudResult.put("status", "ERROR");
        orderCrudResult.put("operation", operation);
        orderCrudResult.put("message", "An unexpected error occurred while processing purchase order operation");
        orderCrudResult.put("errorDetails", errorMessage);
        orderCrudResult.put("timestamp", java.time.LocalDateTime.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("orderCrudResult", orderCrudResult);
        result.put("operationStatus", "ERROR");
        result.put("operation", operation);
        result.put("orderId", null);
        result.put("orderNumber", null);

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

    private LocalDate extractLocalDate(Object value) {
        if (value == null) return null;
        String str = extractString(value);
        if (str == null) return null;
        try { return LocalDate.parse(str); } catch (DateTimeParseException e) { return null; }
    }

    private OrderStatus extractOrderStatus(Object value) {
        String str = extractString(value);
        if (str == null) return null;
        try { return OrderStatus.valueOf(str.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }
}
