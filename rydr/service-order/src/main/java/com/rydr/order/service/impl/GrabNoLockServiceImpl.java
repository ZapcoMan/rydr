package com.rydr.order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rydr.dto.ResponseResult;
import com.rydr.order.service.GrabService;
import com.rydr.order.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Slf4j

/**
 * @deprecated Retained as a reference implementation only. Production traffic uses
 * {@code GrabRedisRedissonServiceImpl} (single Redisson instance) for distributed locking.
 */
@Deprecated
@Service("grabNoLockService")
public class GrabNoLockServiceImpl implements GrabService {

	@Autowired
	OrderService orderService;

	@Override
	public ResponseResult grabOrder(int orderId, int driverId) {

		try {
			log.info("Driver:{} executing order grab logic", driverId);

            boolean b = orderService.grab(orderId, driverId);
            if(b) {
            	log.info("Driver:{} grabbed order {} successfully", driverId, orderId);
            	return ResponseResult.success("");
            } else {
            	log.info("Driver:{} failed to grab order {}", driverId, orderId);
            	return ResponseResult.fail(com.rydr.constatnt.BusinessInterfaceStatus.FAIL.getCode(),
            			"Order already taken or not dispatchable");
            }

        } finally {


        }
	}

}
