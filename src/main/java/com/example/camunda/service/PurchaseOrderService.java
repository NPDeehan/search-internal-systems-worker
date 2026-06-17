package com.example.camunda.service;

import com.example.camunda.exception.PurchaseOrderNotFoundException;
import com.example.camunda.model.OrderStatus;
import com.example.camunda.model.PurchaseItem;
import com.example.camunda.model.PurchaseOrder;
import com.example.camunda.repository.PurchaseOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository orderRepository;
    private final ProductService productService;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    public List<PurchaseOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    public PurchaseOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found with ID: " + orderId));
    }

    public List<PurchaseOrder> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<PurchaseOrder> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<PurchaseOrder> searchOrders(Long customerId, OrderStatus status) {
        Specification<PurchaseOrder> spec = Specification.where(null);
        if (customerId != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("customerId"), customerId));
        if (status     != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"),     status));
        return orderRepository.findAll(spec);
    }

    @Transactional
    public PurchaseOrder saveOrder(PurchaseOrder order) {
        if (order.getOrderId() == null) {
            Long maxId = orderRepository.findMaxOrderId();
            order.setOrderId((maxId == null ? 0L : maxId) + 1L);
        }
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(String.format("PO-%06d", order.getOrderId()));
        }
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.DRAFT);
        }

        // Re-parent each item, fill any missing name/price from the catalog, recompute total
        if (order.getItems() != null) {
            for (PurchaseItem item : order.getItems()) {
                item.setOrder(order);
                if (item.getProductId() != null && (item.getProductName() == null || item.getUnitPrice() == null)) {
                    com.example.camunda.model.Product product = productService.getProductById(item.getProductId());
                    if (item.getProductName() == null) item.setProductName(product.getName());
                    if (item.getUnitPrice() == null)   item.setUnitPrice(product.getRrp());
                }
            }
        }
        recalculateTotal(order);

        log.debug("Saving purchase order: {} ({} item(s), total {})",
                order.getOrderNumber(), order.getItemCount(), order.getTotalAmount());
        return orderRepository.save(order);
    }

    /** Recomputes and stores the order total from its items. Shared with PurchaseItemService. */
    public void recalculateTotal(PurchaseOrder order) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getItems() != null) {
            for (PurchaseItem item : order.getItems()) {
                if (item.getLineTotal() != null) {
                    total = total.add(item.getLineTotal());
                }
            }
        }
        order.setTotalAmount(total);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new PurchaseOrderNotFoundException("Purchase order not found with ID: " + orderId);
        }
        orderRepository.deleteById(orderId);
        log.debug("Deleted purchase order with ID: {}", orderId);
    }
}
