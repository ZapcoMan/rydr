package com.rydr.wallet.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Passenger wallet account, maps to tbl_passenger_wallet.
 *
 * @author oi
 */
public class WalletAccount {

    private Integer id;

    private Integer passengerInfoId;

    private BigDecimal capital;

    private BigDecimal giveFee;

    private BigDecimal freezeCapital;

    private BigDecimal freezeGiveFee;

    private Date createTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPassengerInfoId() {
        return passengerInfoId;
    }

    public void setPassengerInfoId(Integer passengerInfoId) {
        this.passengerInfoId = passengerInfoId;
    }

    public BigDecimal getCapital() {
        return capital;
    }

    public void setCapital(BigDecimal capital) {
        this.capital = capital;
    }

    public BigDecimal getGiveFee() {
        return giveFee;
    }

    public void setGiveFee(BigDecimal giveFee) {
        this.giveFee = giveFee;
    }

    public BigDecimal getFreezeCapital() {
        return freezeCapital;
    }

    public void setFreezeCapital(BigDecimal freezeCapital) {
        this.freezeCapital = freezeCapital;
    }

    public BigDecimal getFreezeGiveFee() {
        return freezeGiveFee;
    }

    public void setFreezeGiveFee(BigDecimal freezeGiveFee) {
        this.freezeGiveFee = freezeGiveFee;
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
