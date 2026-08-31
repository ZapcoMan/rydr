package com.rydr.order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rydr.dto.ResponseResult;
import com.rydr.common.entity.OrderLock;
import com.rydr.order.lock.MysqlLock;
import com.rydr.order.service.GrabService;
import com.rydr.order.service.OrderService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 *
 * @deprecated Retained as a reference implementation only. Production traffic uses
 * {@code GrabRedisRedissonServiceImpl} (single Redisson instance) for distributed locking.
 */
@Deprecated
@Slf4j
@Service("grabMysqlLockService")
public class GrabMysqlLockServiceImpl implements GrabService {

	@Autowired
	private MysqlLock lock;

	@Autowired
	OrderService orderService;

	ThreadLocal<OrderLock> orderLock = new ThreadLocal<>();

    @Override
    public ResponseResult grabOrder(int orderId , int driverId){
        //generate key
        OrderLock ol = new OrderLock();
        ol.setOrderId(orderId);
        ol.setDriverId(driverId);

        orderLock.set(ol);
        lock.setOrderLockThreadLocal(orderLock);
        lock.lock();
//        System.out.println("Driver "+driverId+" locked successfully");

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

            lock.unlock();
        }
    }
}
