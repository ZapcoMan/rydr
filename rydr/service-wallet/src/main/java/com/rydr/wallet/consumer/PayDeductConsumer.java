package com.rydr.wallet.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.rydr.common.util.JsonUtil;
import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.wallet.config.ActiveMQConfig;
import com.rydr.wallet.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consumes payment-deduction commands produced by service-order and performs the
 * (idempotent) wallet deduction. The outcome is published back to
 * {@code queue.wallet.result} so the order service can mark PAID or roll back.
 */
@Slf4j
@Component
public class PayDeductConsumer {

    @Autowired
    private WalletService walletService;

    @Autowired
    private JmsTemplate jmsTemplate;

    @JmsListener(destination = ActiveMQConfig.QUEUE_PAY_DEDUCT,
            containerFactory = "jmsListenerContainerFactory")
    public void onPayCommand(String message) {
        log.info("Received pay command: {}", message);
        JsonNode node = JsonUtil.parse(message);

        int orderId = node.path("orderId").asInt();
        int passengerId = node.path("passengerId").asInt();
        BigDecimal amount = node.has("amount")
                ? node.path("amount").decimalValue()
                : BigDecimal.ZERO;
        String bizKey = "PAY_" + orderId;

        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("orderId", orderId);
        reply.put("bizKey", bizKey);
        reply.put("success", false);
        reply.put("payType", 1);

        try {
            ResponseResult<Void> result = walletService.payOrder(passengerId, orderId, amount);
            if (result != null && result.getCode() == BusinessInterfaceStatus.SUCCESS.getCode()) {
                reply.put("success", true);
            } else {
                reply.put("reason", result == null ? "wallet returned null" : result.getMessage());
            }
        } catch (Exception e) {
            reply.put("reason", e.getMessage());
            log.error("Wallet deduction failed for order {}", orderId, e);
        }

        jmsTemplate.convertAndSend(ActiveMQConfig.QUEUE_WALLET_RESULT, JsonUtil.toJson(reply));
        log.info("Published wallet result for order {}: success={}",
                orderId, reply.get("success"));
    }
}
