package com.shopmart.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Không tìm thấy đơn hàng " + orderId);
    }
}
