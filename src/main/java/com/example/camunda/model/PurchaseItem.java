package com.example.camunda.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * A single purchased line on a {@link PurchaseOrder}. References a catalog {@link Product}
 * and captures the quantity and unit price at order time.
 */
@Entity
@Table(name = "purchase_items")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long purchaseItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private PurchaseOrder order;

    // Read-only mirror of the order FK so the owning order id is always available
    // (in JSON and after a DB load) without forcing the lazy association.
    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    // References Product.productId — the catalog product being purchased
    @Column(nullable = false)
    private Long productId;

    // Snapshot of the product name at order time (denormalized for display)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    // Price per unit captured at order time
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    // --- Getters and Setters ---

    public Long getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Long purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    @JsonIgnore
    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder order) { this.order = order; }

    public Long getOrderId() {
        if (orderId != null) return orderId;
        return order != null ? order.getOrderId() : null;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    @Transient
    public BigDecimal getLineTotal() {
        if (unitPrice == null || quantity == null) return null;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
