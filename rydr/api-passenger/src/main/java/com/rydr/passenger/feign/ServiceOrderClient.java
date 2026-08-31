package com.rydr.passenger.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.rydr.dto.ResponseResult;
import com.rydr.entity.Order;

import java.util.List;
import java.util.Map;

/**
 * Feign client to the order service.
 *
 * @author oi
 */
@FeignClient(name = "service-order")
public interface ServiceOrderClient {

	@PostMapping("/order/create")
	ResponseResult<Order> create(@RequestBody Order order);

	@GetMapping("/order/number/{orderNumber}")
	ResponseResult<Order> getByNumber(@PathVariable("orderNumber") String orderNumber);

	@GetMapping("/order/passenger/{passengerInfoId}")
	ResponseResult<List<Order>> listByPassenger(
			@PathVariable("passengerInfoId") int passengerInfoId);

	@PostMapping("/order/start/{orderId}")
	ResponseResult<Boolean> startTrip(@PathVariable("orderId") int orderId);

	@PostMapping("/order/end/{orderId}")
	ResponseResult<Boolean> endTrip(@PathVariable("orderId") int orderId);

	@PostMapping("/order/pay/{orderId}")
	ResponseResult<Map<String, Object>> pay(@PathVariable("orderId") int orderId,
											@RequestParam("payType") int payType);

	@PostMapping("/order/cancel/{orderId}")
	ResponseResult<Boolean> cancel(@PathVariable("orderId") int orderId,
								   @RequestParam("cancelType") int cancelType);
}
