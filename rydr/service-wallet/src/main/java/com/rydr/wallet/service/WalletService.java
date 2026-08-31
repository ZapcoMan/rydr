package com.rydr.wallet.service;

import com.rydr.dto.ResponseResult;
import com.rydr.wallet.entity.WalletAccount;

import java.math.BigDecimal;

/**
 * Passenger wallet business operations.
 *
 * @author oi
 */
public interface WalletService {

    /**
     * Query the wallet balance for a passenger, creating the account on first access.
     */
    ResponseResult<WalletAccount> getBalance(Integer passengerInfoId);

    /**
     * Create a recharge order and return the out_trade_no to hand to the payment gateway.
     */
    ResponseResult<String> createRecharge(Integer passengerInfoId, BigDecimal amount, Integer payType);

    /**
     * Apply a payment-gateway callback. Idempotent: the same out_trade_no is only credited once.
     */
    ResponseResult<Void> handleRechargeCallback(String outTradeNo, String tradeNo);

    /**
     * Pay an order from the wallet balance. Idempotent per orderId and concurrency-safe
     * (no over-deduction). Returns success only when the balance was sufficient.
     */
    ResponseResult<Void> payOrder(Integer passengerInfoId, Integer orderId, BigDecimal amount);
}
