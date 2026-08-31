package com.rydr.order.service.impl;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.rydr.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import com.rydr.common.entity.OrderLock;
import com.rydr.order.lock.MysqlLock;
import com.rydr.order.lock.RedisLock;
import com.rydr.order.service.GrabService;
import com.rydr.order.service.OrderService;

/**
 * @author oi
 */
@Service("grabRedisRedissonService")
@Slf4j
public class GrabRedisRedissonServiceImpl implements GrabService {

	@Autowired
	@Qualifier("redissonClient")
	RedissonClient redissonClient;

	@Autowired
	OrderService orderService;

    @Override
    public ResponseResult grabOrder(int orderId , int driverId){
        //generate key
    	String lock = "order_"+(orderId+"");

    	RLock rlock = redissonClient.getLock(lock.intern());


    	try {
    		// This code by default sets key timeout to 30 seconds, and renews after 10 seconds
    		rlock.lock();
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
        	rlock.unlock();
        }
    }
}
