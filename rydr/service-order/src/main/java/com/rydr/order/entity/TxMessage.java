package com.rydr.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Reliable-message outbox record (table: tx_message).
 *
 * <p>Supports the "local DB commit + JMS send" pattern: an outbound message is
 * written to this table inside the same local transaction as the business update,
 * then a JMS transaction delivers it to ActiveMQ. A scheduled compensation job
 * re-delivers INIT / overdue rows until the consumer confirms.
 *
 * <p>Status codes: 0 INIT, 1 SENT, 2 DONE, 3 FAIL.
 */
@Data
public class TxMessage {

    public static final int STATUS_INIT = 0;
    public static final int STATUS_SENT = 1;
    /** Terminal success: the consumer confirmed the business effect. */
    public static final int STATUS_DONE = 2;
    /** Delivery attempt failed; the compensation job retries with backoff. */
    public static final int STATUS_FAIL = 3;
    /**
     * Terminal business failure (e.g. insufficient balance): the record is kept for
     * audit but is NOT retried automatically - the user must top up and pay again.
     */
    public static final int STATUS_ABANDONED = 4;

    private Long id;

    /** Unique business key, e.g. PAY_&lt;orderId&gt;. */
    private String bizKey;

    /** JMS destination (queue) name. */
    private String topic;

    /** JSON business payload. */
    private String payload;

    private Integer status;

    private Integer retry;

    /** Next delivery time (for backoff); null means ready now. */
    private Date nextRetryAt;

    private Date createTime;

    private Date updateTime;

    /** Transient convenience field parsed from payload (not persisted). */
    private transient BigDecimal fare;
}
