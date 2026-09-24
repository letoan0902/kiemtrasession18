package com.shopmart.order.exception;

public class ProductNotFoundException extends InventoryBusinessException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
