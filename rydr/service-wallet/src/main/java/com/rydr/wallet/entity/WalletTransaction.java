package com.rydr.wallet.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet transaction (flow) record. One row per balance change, used for reconciliation.
 * The {@code bizNo} column is UNIQUE so the same business event is never applied twice.
 *
 * @author oi
 */
public class WalletTransaction {

    public static final int TYPE_RECHARGE = 1;
    public static final int TYPE_PAY = 2;
    public static final int TYPE_REFUND = 3;
    public static final int TYPE_FREEZE = 4;
    public static final int TYPE_UNFREEZE = 5;

    private Long id;

    private Integer passengerInfoId;

    /** Unique business number (out_trade_no / order payment id) for idempotency. */
    private String bizNo;

    private Integer type;

    /** Positive for inflow (recharge/refund), negative for outflow (pay/freeze). */
    private BigDecimal amount;

    private BigDecimal capitalBefore;

    private BigDecimal capitalAfter;

    private String remark;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPassengerInfoId() {
        return passengerInfoId;
    }

    public void setPassengerInfoId(Integer passengerInfoId) {
        this.passengerInfoId = passengerInfoId;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCapitalBefore() {
        return capitalBefore;
    }

    public void setCapitalBefore(BigDecimal capitalBefore) {
        this.capitalBefore = capitalBefore;
    }

    public BigDecimal getCapitalAfter() {
        return capitalAfter;
    }

    public void setCapitalAfter(BigDecimal capitalAfter) {
        this.capitalAfter = capitalAfter;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
