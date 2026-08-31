package com.rydr.order.controller;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.entity.Order;
import com.rydr.order.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Order lifecycle API (called by api-passenger / api-driver through the gateway).
 *
 * @author oi
 */
@RestController
@RequestMapping("/order")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@PostMapping("/create")
	public ResponseResult<Order> create(@RequestBody Order order) {
		Order created = orderService.createOrder(order);
		return ResponseResult.success(created);
	}

	@GetMapping("/number/{orderNumber}")
	public ResponseResult<Order> getByNumber(@PathVariable("orderNumber") String orderNumber) {
		return ResponseResult.success(orderService.getByOrderNumber(orderNumber));
	}

	@GetMapping("/passenger/{passengerInfoId}")
	public ResponseResult<List<Order>> listByPassenger(
			@PathVariable("passengerInfoId") int passengerInfoId) {
		return ResponseResult.success(orderService.listByPassenger(passengerInfoId));
	}

	@PostMapping("/start/{orderId}")
	public ResponseResult<Boolean> startTrip(@PathVariable("orderId") int orderId) {
		return ResponseResult.success(orderService.startTrip(orderId));
	}

	@PostMapping("/end/{orderId}")
	public ResponseResult<Boolean> endTrip(@PathVariable("orderId") int orderId) {
		return ResponseResult.success(orderService.endTrip(orderId));
	}

	@PostMapping("/pay/{orderId}")
	public ResponseResult<Map<String, Object>> pay(@PathVariable("orderId") int orderId,
												   @RequestParam("payType") int payType) {
		Map<String, Object> result = orderService.pay(orderId, payType);
		boolean ok = Boolean.TRUE.equals(result.get("success"));
		int code = ok ? BusinessInterfaceStatus.SUCCESS.getCode() : BusinessInterfaceStatus.FAIL.getCode();
		return new ResponseResult<Map<String, Object>>()
				.setCode(code)
				.setMessage((String) result.getOrDefault("message", ""))
				.setData(result);
	}

	@PostMapping("/cancel/{orderId}")
	public ResponseResult<Boolean> cancel(@PathVariable("orderId") int orderId,
										  @RequestParam("cancelType") int cancelType) {
		return ResponseResult.success(orderService.cancel(orderId, cancelType));
	}
}
