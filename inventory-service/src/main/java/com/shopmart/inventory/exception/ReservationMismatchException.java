package com.shopmart.inventory.exception;

public class ReservationMismatchException extends RuntimeException {

    public ReservationMismatchException(String orderId, Long reservedProductId, Long requestedProductId) {
        super("Đơn " + orderId + " đã trừ kho sản phẩm " + reservedProductId
                + ", không phải sản phẩm " + requestedProductId);
    }
}
