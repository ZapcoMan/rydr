package com.rydr.order.job;

import com.rydr.order.config.ActiveMQTxConfig;
import com.rydr.order.dao.TxMessageMapper;
import com.rydr.order.entity.TxMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Compensation job for the reliable-message outbox.
 *
 * <p>Every {@code intervalMs} it re-delivers outbox rows still in INIT/SENT whose
 * {@code next_retry_at} has passed. Combined with the wallet consumer's idempotency
 * (biz_no unique), this yields at-least-once delivery with exactly-once effect, closing
 * the final-consistency loop when a synchronous JMS send failed earlier.
 */
@Slf4j
@Component
@EnableScheduling
public class TxMessageCompensationJob {

    /** Max deliveries per scan; also guards against a poison message hogging the loop. */
    private static final int MAX_DELIVERIES_PER_SCAN = 50;

    /** Exponential backoff: next_retry_at = now + retryBaseMillis * 2^retry. */
    private static final int RETRY_BASE_MILLIS = 10_000;

    @Autowired
    private TxMessageMapper txMessageMapper;

    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    @Value("${tx-message.compensation-interval-ms:30000}")
    private long intervalMs;

    @Scheduled(fixedDelayString = "${tx-message.compensation-interval-ms:30000}")
    public void redeliver() {
        if (jmsTemplate == null) {
            return;
        }
        List<TxMessage> due = txMessageMapper.selectDue(MAX_DELIVERIES_PER_SCAN);
        if (due.isEmpty()) {
            return;
        }
        log.info("Compensation scan: {} outbox message(s) due for redelivery", due.size());
        for (TxMessage msg : due) {
            redeliverOne(msg);
        }
    }

    private void redeliverOne(TxMessage msg) {
        try {
            jmsTemplate.convertAndSend(ActiveMQTxConfig.QUEUE_PAY_DEDUCT, msg.getPayload());
            txMessageMapper.updateStatus(msg.getId(), TxMessage.STATUS_SENT, msg.getRetry(), null);
            log.info("Redelivered outbox message id={} bizKey={}", msg.getId(), msg.getBizKey());
        } catch (Exception e) {
            // Backoff then give up after a bounded number of attempts.
            int retry = (msg.getRetry() == null ? 0 : msg.getRetry()) + 1;
            Date next = new Date(System.currentTimeMillis() + RETRY_BASE_MILLIS * (1L << Math.min(retry, 10)));
            txMessageMapper.updateStatus(msg.getId(), TxMessage.STATUS_FAIL, retry, next);
            log.warn("Redelivery failed for id={} bizKey={}, will retry at {}",
                    msg.getId(), msg.getBizKey(), next, e);
        }
    }

    @SuppressWarnings("unused")
    public long getIntervalMs() {
        return intervalMs;
    }
}
