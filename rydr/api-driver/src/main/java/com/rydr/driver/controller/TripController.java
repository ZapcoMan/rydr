package com.rydr.driver.controller;

import com.rydr.dto.ResponseResult;
import com.rydr.driver.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driver trip lifecycle: start a trip when the passenger boards, end it when dropped off.
 *
 * @author oi
 */
@RestController
@RequestMapping("/trip")
public class TripController {

	@Autowired
	private OrderService orderService;

	@PostMapping("/start/{orderId}")
	public ResponseResult<Boolean> start(@PathVariable("orderId") int orderId) {
		return ResponseResult.success(orderService.startTrip(orderId));
	}

	@PostMapping("/end/{orderId}")
	public ResponseResult<Boolean> end(@PathVariable("orderId") int orderId) {
		return ResponseResult.success(orderService.endTrip(orderId));
	}
}
