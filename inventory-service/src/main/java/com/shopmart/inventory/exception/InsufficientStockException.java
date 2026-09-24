package com.shopmart.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Không đủ hàng cho sản phẩm " + productId + ": cần " + requested + ", chỉ còn " + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
