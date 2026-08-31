package com.rydr.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rydr.dto.ResponseResult;

import java.math.BigDecimal;

/**
 * Feign client to the wallet service for payment settlement.
 *
 * @author oi
 */
@FeignClient(name = "service-wallet")
public interface WalletServiceClient {

	@PostMapping("/wallet/pay")
	ResponseResult<Void> pay(@RequestParam("passengerId") Integer passengerId,
							 @RequestParam("orderId") Integer orderId,
							 @RequestParam("amount") BigDecimal amount);
}
