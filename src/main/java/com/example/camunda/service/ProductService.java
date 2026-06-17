package com.example.camunda.service;

import com.example.camunda.exception.ProductNotFoundException;
import com.example.camunda.model.Product;
import com.example.camunda.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    }

    public List<Product> searchProducts(String name, String category, String tier, Boolean inStock) {
        Specification<Product> spec = Specification.where(null);
        if (name     != null) spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        if (category != null) spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        if (tier     != null) spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("tier")), tier.toLowerCase()));
        if (inStock  != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("inStock"), inStock));
        return productRepository.findAll(spec);
    }

    @Transactional
    public Product saveProduct(Product product) {
        if (product.getProductId() == null) {
            Long maxId = productRepository.findMaxProductId();
            product.setProductId((maxId == null ? 0L : maxId) + 1L);
        }
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku(String.format("PRD-%06d", product.getProductId()));
        }
        if (product.getInStock() == null) {
            product.setInStock(product.getStockQty() != null && product.getStockQty() > 0);
        }
        log.debug("Saving product: {}", product.getSku());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Product not found with ID: " + productId);
        }
        productRepository.deleteById(productId);
        log.debug("Deleted product with ID: {}", productId);
    }
}
