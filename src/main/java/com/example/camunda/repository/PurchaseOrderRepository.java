package com.example.camunda.repository;

import com.example.camunda.model.OrderStatus;
import com.example.camunda.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    List<PurchaseOrder> findByCustomerId(Long customerId);

    List<PurchaseOrder> findByStatus(OrderStatus status);

    @Query("SELECT COALESCE(MAX(o.orderId), 0) FROM PurchaseOrder o")
    Long findMaxOrderId();
}
