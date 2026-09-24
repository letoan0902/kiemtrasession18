package com.shopmart.inventory.cache;

import com.shopmart.inventory.domain.Product;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class ProductCacheData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private long price;
    private int stock;
    private LocalDateTime updatedAt;

    public ProductCacheData() {
    }

    public ProductCacheData(Long id, String name, long price, int stock, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.updatedAt = updatedAt;
    }

    public static ProductCacheData from(Product product) {
        return new ProductCacheData(product.getId(), product.getName(), product.getPrice(),
                product.getStock(), product.getUpdatedAt());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
