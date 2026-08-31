package com.rydr.order.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.rydr.common.util.JsonUtil;
import com.rydr.order.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes wallet settlement results (success / failure) sent back by service-wallet.
 *
 * <p>On success the order is marked PAID and the outbox row DONE; on failure the order
 * status is rolled back to allow a retry. Listening is transactional (see
 * {@code ActiveMQTxConfig}), so the ack is deferred until the handler commits.
 */
@Slf4j
@Component
public class WalletResultConsumer {

    private static final String DESTINATION = "queue.wallet.result";

    @Autowired
    private OrderServiceImpl orderService;

    @JmsListener(destination = DESTINATION, containerFactory = "jmsListenerContainerFactory")
    public void onWalletResult(String message) {
        log.info("Received wallet result: {}", message);
        JsonNode node = JsonUtil.parse(message);

        int orderId = node.path("orderId").asInt();
        String bizKey = node.path("bizKey").asText();
        boolean success = node.path("success").asBoolean(false);
        int payType = node.path("payType").asInt(1);
        String reason = node.path("reason").asText("");

        if (success) {
            orderService.onPaySuccess(orderId, payType, bizKey);
        } else {
            orderService.onPayFailure(orderId, reason, bizKey);
        }
    }
}
