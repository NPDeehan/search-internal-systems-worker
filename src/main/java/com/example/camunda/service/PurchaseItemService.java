package com.example.camunda.service;

import com.example.camunda.exception.PurchaseItemNotFoundException;
import com.example.camunda.model.Product;
import com.example.camunda.model.PurchaseItem;
import com.example.camunda.model.PurchaseOrder;
import com.example.camunda.repository.PurchaseItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PurchaseItemService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseItemService.class);

    private final PurchaseItemRepository itemRepository;
    private final PurchaseOrderService orderService;
    private final ProductService productService;

    public PurchaseItemService(PurchaseItemRepository itemRepository,
                               PurchaseOrderService orderService,
                               ProductService productService) {
        this.itemRepository = itemRepository;
        this.orderService = orderService;
        this.productService = productService;
    }

    public List<PurchaseItem> getItemsByOrder(Long orderId) {
        // getOrderById throws PurchaseOrderNotFoundException (404) when the order is missing
        orderService.getOrderById(orderId);
        return itemRepository.findByOrder_OrderId(orderId);
    }

    public PurchaseItem getItemById(Long purchaseItemId) {
        return itemRepository.findById(purchaseItemId)
                .orElseThrow(() -> new PurchaseItemNotFoundException("Purchase item not found with ID: " + purchaseItemId));
    }

    /**
     * Adds a new item to an existing order. Missing unitPrice/productName are resolved from
     * the catalog product. The order total is recomputed and persisted.
     */
    @Transactional
    public PurchaseItem addItem(Long orderId, Long productId, Integer quantity, BigDecimal unitPrice) {
        PurchaseOrder order = orderService.getOrderById(orderId);
        Product product = productService.getProductById(productId); // 404 if unknown product

        PurchaseItem item = new PurchaseItem();
        item.setOrder(order);
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setQuantity(quantity == null || quantity <= 0 ? 1 : quantity);
        item.setUnitPrice(unitPrice != null ? unitPrice : product.getRrp());

        // Persist the item directly so its generated id is populated immediately
        item = itemRepository.saveAndFlush(item);
        order.getItems().add(item);
        orderService.recalculateTotal(order); // order is managed in this tx → flushed on commit
        log.debug("Added item {} (product={} qty={}) to order {}", item.getPurchaseItemId(), productId, item.getQuantity(), orderId);
        return item;
    }

    @Transactional
    public PurchaseItem updateItem(Long purchaseItemId, Long productId, Integer quantity, BigDecimal unitPrice) {
        PurchaseItem item = getItemById(purchaseItemId);
        if (productId != null) {
            Product product = productService.getProductById(productId);
            item.setProductId(productId);
            item.setProductName(product.getName());
        }
        if (quantity != null && quantity > 0) item.setQuantity(quantity);
        if (unitPrice != null) item.setUnitPrice(unitPrice);

        orderService.saveOrder(item.getOrder()); // recomputes total + persists the change
        log.debug("Updated purchase item {}", purchaseItemId);
        return item;
    }

    @Transactional
    public void deleteItem(Long purchaseItemId) {
        PurchaseItem item = getItemById(purchaseItemId);
        PurchaseOrder order = item.getOrder();
        order.getItems().removeIf(i -> i.getPurchaseItemId().equals(purchaseItemId));
        orderService.saveOrder(order); // orphanRemoval deletes the item + recomputes total
        log.debug("Deleted purchase item {} from order {}", purchaseItemId,
                order != null ? order.getOrderId() : null);
    }
}
