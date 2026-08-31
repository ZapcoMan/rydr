package com.rydr.wallet.controller;

import com.rydr.dto.ResponseResult;
import com.rydr.wallet.entity.WalletAccount;
import com.rydr.wallet.service.WalletService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Passenger wallet REST API.
 *
 * @author oi
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/balance")
    public ResponseResult<WalletAccount> balance(@RequestParam("passengerId") Integer passengerId) {
        return walletService.getBalance(passengerId);
    }

    @PostMapping("/recharge/create")
    public ResponseResult<String> createRecharge(@RequestParam("passengerId") Integer passengerId,
                                                 @RequestParam("amount") BigDecimal amount,
                                                 @RequestParam(value = "payType", defaultValue = "2") Integer payType) {
        return walletService.createRecharge(passengerId, amount, payType);
    }

    @PostMapping("/recharge/callback")
    public ResponseResult<Void> rechargeCallback(@RequestParam("outTradeNo") String outTradeNo,
                                                @RequestParam(value = "tradeNo", required = false) String tradeNo) {
        return walletService.handleRechargeCallback(outTradeNo, tradeNo);
    }

    @PostMapping("/pay")
    public ResponseResult<Void> pay(@RequestParam("passengerId") Integer passengerId,
                                   @RequestParam("orderId") Integer orderId,
                                   @RequestParam("amount") BigDecimal amount) {
        return walletService.payOrder(passengerId, orderId, amount);
    }
}
