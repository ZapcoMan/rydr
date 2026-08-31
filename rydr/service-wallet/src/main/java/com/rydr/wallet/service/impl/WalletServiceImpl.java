package com.rydr.wallet.service.impl;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.wallet.entity.WalletAccount;
import com.rydr.wallet.entity.WalletRecharge;
import com.rydr.wallet.entity.WalletTransaction;
import com.rydr.wallet.mapper.WalletAccountMapper;
import com.rydr.wallet.mapper.WalletRechargeMapper;
import com.rydr.wallet.mapper.WalletTransactionMapper;
import com.rydr.wallet.service.WalletService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * Wallet service implementation.
 *
 * <p>Concurrency &amp; idempotency guarantees:
 * <ul>
 *   <li>Deduction uses a single conditional UPDATE {@code capital = capital - ? WHERE capital >= ?}
 *       so two concurrent payments can never drive the balance negative (no over-deduction).</li>
 *   <li>Every balance change writes a {@link WalletTransaction} keyed by a UNIQUE {@code biz_no};
 *       a second application of the same business event is detected and skipped.</li>
 *   <li>Recharge callbacks are de-duplicated by the gateway {@code out_trade_no}.</li>
 * </ul>
 *
 * @author oi
 */
@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletAccountMapper walletAccountMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Autowired
    private WalletRechargeMapper walletRechargeMapper;

    @Override
    public ResponseResult<WalletAccount> getBalance(Integer passengerInfoId) {
        WalletAccount account = getOrCreateAccount(passengerInfoId);
        return ResponseResult.success(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<String> createRecharge(Integer passengerInfoId, BigDecimal amount, Integer payType) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "Recharge amount must be positive");
        }
        getOrCreateAccount(passengerInfoId);

        String outTradeNo = "RC" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        WalletRecharge recharge = new WalletRecharge();
        recharge.setPassengerInfoId(passengerInfoId);
        recharge.setOutTradeNo(outTradeNo);
        recharge.setAmount(amount);
        recharge.setPayType(payType);
        recharge.setStatus(WalletRecharge.STATUS_INIT);
        recharge.setCreateTime(new Date());
        recharge.setUpdateTime(new Date());
        walletRechargeMapper.insert(recharge);

        return ResponseResult.success(outTradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> handleRechargeCallback(String outTradeNo, String tradeNo) {
        if (walletTransactionMapper.selectByBizNo(outTradeNo) != null) {
            // Already credited for this recharge; idempotent no-op.
            return ResponseResult.success();
        }

        WalletRecharge recharge = walletRechargeMapper.selectByOutTradeNo(outTradeNo);
        if (recharge == null) {
            return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "Unknown recharge order: " + outTradeNo);
        }
        if (recharge.getStatus() == WalletRecharge.STATUS_PAID) {
            return ResponseResult.success();
        }

        // Mark the recharge paid first (conditional update guards against double callback).
        int updated = walletRechargeMapper.updateStatus(outTradeNo, WalletRecharge.STATUS_PAID, tradeNo);
        if (updated == 0) {
            return ResponseResult.success();
        }

        WalletAccount account = getOrCreateAccount(recharge.getPassengerInfoId());
        BigDecimal before = account.getCapital() == null ? BigDecimal.ZERO : account.getCapital();
        walletAccountMapper.addCapital(recharge.getPassengerInfoId(), recharge.getAmount());
        BigDecimal after = before.add(recharge.getAmount());

        WalletTransaction tx = new WalletTransaction();
        tx.setPassengerInfoId(recharge.getPassengerInfoId());
        tx.setBizNo(outTradeNo);
        tx.setType(WalletTransaction.TYPE_RECHARGE);
        tx.setAmount(recharge.getAmount());
        tx.setCapitalBefore(before);
        tx.setCapitalAfter(after);
        tx.setRemark("recharge");
        tx.setCreateTime(new Date());
        walletTransactionMapper.insert(tx);

        log.info("Recharge credited {} to passenger {} (outTradeNo={})", recharge.getAmount(),
                recharge.getPassengerInfoId(), outTradeNo);
        return ResponseResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> payOrder(Integer passengerInfoId, Integer orderId, BigDecimal amount) {
        String bizNo = "PAY" + orderId;
        if (walletTransactionMapper.selectByBizNo(bizNo) != null) {
            // Already paid for this order; idempotent no-op.
            return ResponseResult.success();
        }

        WalletAccount account = getOrCreateAccount(passengerInfoId);
        BigDecimal before = account.getCapital() == null ? BigDecimal.ZERO : account.getCapital();
        if (before.compareTo(amount) < 0) {
            return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "Insufficient wallet balance");
        }

        // Concurrency-safe deduction: only succeeds when balance >= amount.
        int deducted = walletAccountMapper.deductCapital(passengerInfoId, amount);
        if (deducted == 0) {
            return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "Insufficient wallet balance");
        }

        BigDecimal after = before.subtract(amount);
        WalletTransaction tx = new WalletTransaction();
        tx.setPassengerInfoId(passengerInfoId);
        tx.setBizNo(bizNo);
        tx.setType(WalletTransaction.TYPE_PAY);
        tx.setAmount(amount.negate());
        tx.setCapitalBefore(before);
        tx.setCapitalAfter(after);
        tx.setRemark("pay order " + orderId);
        tx.setCreateTime(new Date());
        walletTransactionMapper.insert(tx);

        log.info("Paid {} from passenger {} for order {} (balance {} -> {})", amount,
                passengerInfoId, orderId, before, after);
        return ResponseResult.success();
    }

    private WalletAccount getOrCreateAccount(Integer passengerInfoId) {
        WalletAccount account = walletAccountMapper.selectByPassengerId(passengerInfoId);
        if (account == null) {
            account = new WalletAccount();
            account.setPassengerInfoId(passengerInfoId);
            account.setCapital(BigDecimal.ZERO);
            account.setGiveFee(BigDecimal.ZERO);
            account.setFreezeCapital(BigDecimal.ZERO);
            account.setFreezeGiveFee(BigDecimal.ZERO);
            account.setCreateTime(new Date());
            account.setUpdateTime(new Date());
            walletAccountMapper.insert(account);
            account = walletAccountMapper.selectByPassengerId(passengerInfoId);
        }
        return account;
    }
}
