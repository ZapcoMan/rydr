package com.rydr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rydr.common.constant.RedisKeyConstant;
import com.rydr.listen.service.ListenService;

@RestController
@RequestMapping("/order")
public class SendOrderController {

	@Autowired
    private ListenService listenService;

	@Autowired
    private RedisTemplate<String,String> redisTemplate;

	/**
	 * Demo endpoint that simulates dispatching an order to a driver.
	 * The Redis value must be the order id, because {@code ListenService} parses it as such.
	 */
	@GetMapping("/send")
	public String sendOrder(String driverId, String orderId) {

		if (driverId == null || driverId.isBlank()) {
			return "driverId is required";
		}

		String key = RedisKeyConstant.DRIVER_LISTEN_ORDER_PRE + driverId;
		String value = (orderId == null || orderId.isBlank()) ? driverId : orderId;
		redisTemplate.opsForValue().set(key, value);

		return "Successfully sent order "+value+" to driver "+driverId;
	}
}
