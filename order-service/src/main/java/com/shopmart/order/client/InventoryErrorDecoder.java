package com.shopmart.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.order.exception.InsufficientStockException;
import com.shopmart.order.exception.InventoryUnavailableException;
import com.shopmart.order.exception.ProductNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InventoryErrorDecoder implements ErrorDecoder {

    private static final Pattern PRODUCT_ID = Pattern.compile("/products/(\\d+)");
    private static final int MAX_BODY_BYTES = 4096;

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public InventoryErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String url = response.request() != null ? response.request().url() : "";
        String bodyMessage = readMessage(response);

        if (status == 404) {
            String productId = extractProductId(url);
            String fallbackMessage = "Không tìm thấy sản phẩm" + (productId != null ? " có mã " + productId : "");
            return new ProductNotFoundException(bodyMessage != null ? bodyMessage : fallbackMessage);
        }
        if (status == 409) {
            return new InsufficientStockException(bodyMessage != null ? bodyMessage : "Không đủ hàng trong kho");
        }
        if (status >= 500) {
            return new InventoryUnavailableException("Kho hàng trả lỗi HTTP " + status
                    + (bodyMessage != null ? ": " + bodyMessage : ""));
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String readMessage(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream in = response.body().asInputStream()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES);
            if (bytes.length == 0) {
                return null;
            }
            JsonNode node = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
            JsonNode message = node != null ? node.get("message") : null;
            return message != null && message.isTextual() ? message.asText() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String extractProductId(String url) {
        Matcher matcher = PRODUCT_ID.matcher(url == null ? "" : url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
