package com.shopmart.payment.messaging;

import com.shopmart.payment.config.PaymentKafkaProperties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "shopmart.payment.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentTopicInitializer {

    private static final Logger log = LoggerFactory.getLogger(PaymentTopicInitializer.class);

    private final PaymentKafkaProperties props;

    public PaymentTopicInitializer(PaymentKafkaProperties props) {
        this.props = props;
    }

    public void ensurePaymentTopic() {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers(),
                AdminClientConfig.CLIENT_ID_CONFIG, "payment-service-admin",
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);
        NewTopic topic = new NewTopic(props.paymentTopic(), props.paymentTopicPartitions(), (short) 1);
        try (AdminClient admin = AdminClient.create(config)) {
            admin.createTopics(List.of(topic)).all().get(15, TimeUnit.SECONDS);
            log.info("Đã tạo topic '{}' với {} partition", props.paymentTopic(), props.paymentTopicPartitions());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.info("Topic '{}' đã tồn tại, không cần tạo", props.paymentTopic());
            } else {
                log.warn("Không tạo được topic '{}': {}", props.paymentTopic(), String.valueOf(e.getCause()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Bị ngắt khi tạo topic '{}'", props.paymentTopic());
        } catch (Exception e) {
            log.warn("Không tạo được topic '{}': {}", props.paymentTopic(), e.toString());
        }
    }
}
