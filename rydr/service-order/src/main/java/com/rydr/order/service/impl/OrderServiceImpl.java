package com.rydr.order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rydr.entity.Order;
import com.rydr.order.dao.OrderMapper;
import com.rydr.order.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderMapper mapper;

	public boolean grab(int orderId, int driverId) {
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order == null) {
			log.warn("Order {} does not exist, driver {} cannot grab it", orderId, driverId);
			return false;
		}

		// Deliberate delay: keeps the lock contention observable in this teaching sample
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Grab interrupted, orderId={}, driverId={}", orderId, driverId);
			return false;
		}

		if (order.getStatus() != null && order.getStatus().intValue() == 0) {
			order.setStatus(1);
			mapper.updateByPrimaryKeySelective(order);

			return true;
		}
		return false;

	}
}
