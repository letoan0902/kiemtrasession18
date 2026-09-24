package com.shopmart.inventory.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Không tìm thấy sản phẩm có mã " + productId);
    }
}
