package com.rydr.wallet.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet recharge order. The {@code outTradeNo} column is UNIQUE so a payment gateway
 * callback is never applied more than once.
 *
 * @author oi
 */
public class WalletRecharge {

    public static final int STATUS_INIT = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_FAILED = 2;

    private Long id;

    private Integer passengerInfoId;

    /** Merchant order number, unique, from the payment gateway. */
    private String outTradeNo;

    private BigDecimal amount;

    private Integer payType;

    private Integer status;

    private String tradeNo;

    private Date paidTime;

    private Date createTime;

    private Date updateTime;

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

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public Date getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(Date paidTime) {
        this.paidTime = paidTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
