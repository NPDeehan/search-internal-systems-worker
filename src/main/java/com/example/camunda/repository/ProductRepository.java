package com.example.camunda.repository;

import com.example.camunda.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    @Query("SELECT COALESCE(MAX(p.productId), 0) FROM Product p")
    Long findMaxProductId();
}
