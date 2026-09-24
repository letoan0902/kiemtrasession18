package com.shopmart.payment.web;

import com.shopmart.payment.web.PaymentDtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e, ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badArgument(IllegalArgumentException e, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> badInput(ServerWebInputException e, ServerWebExchange exchange) {
        String detail = e.getMethodParameter() != null && e.getMethodParameter().getParameterName() != null
                ? " '" + e.getMethodParameter().getParameterName() + "'" : "";
        return build(HttpStatus.BAD_REQUEST, "Tham số" + detail + " bị thiếu hoặc không đúng định dạng", exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> status(ResponseStatusException e, ServerWebExchange exchange) {
        HttpStatusCode code = e.getStatusCode();
        String message = code.value() == 404 ? "Không tìm thấy đường dẫn yêu cầu"
                : e.getReason() != null ? e.getReason() : "Yêu cầu không hợp lệ";
        return build(code, message, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, ServerWebExchange exchange) {
        log.error("Lỗi không mong đợi khi xử lý {}", exchange.getRequest().getPath(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống của payment-service, vui lòng thử lại sau", exchange);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatusCode code, String message, ServerWebExchange exchange) {
        HttpStatus known = HttpStatus.resolve(code.value());
        String error = known != null ? known.getReasonPhrase() : String.valueOf(code.value());
        return ResponseEntity.status(code).body(new ErrorResponse(Instant.now(), code.value(), error, message,
                exchange.getRequest().getPath().value()));
    }
}
