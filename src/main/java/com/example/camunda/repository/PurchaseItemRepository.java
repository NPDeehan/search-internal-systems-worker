package com.example.camunda.repository;

import com.example.camunda.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByOrder_OrderId(Long orderId);
}
