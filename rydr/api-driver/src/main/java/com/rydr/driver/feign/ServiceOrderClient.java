package com.rydr.driver.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.rydr.dto.ResponseResult;
import com.rydr.entity.Order;

/**
 * Feign client to the order service.
 *
 * @author oi
 */
@FeignClient(name = "service-order")
public interface ServiceOrderClient {

	@PostMapping("/order/start/{orderId}")
	ResponseResult<Boolean> startTrip(@PathVariable("orderId") int orderId);

	@PostMapping("/order/end/{orderId}")
	ResponseResult<Boolean> endTrip(@PathVariable("orderId") int orderId);

	@PostMapping("/order/create")
	ResponseResult<Order> create(@RequestBody Order order);
}
