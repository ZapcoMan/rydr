package com.rydr.order.service.impl;

import com.rydr.common.constant.RedisKeyConstant;
import com.rydr.dto.ResponseResult;
import com.rydr.order.service.GrabService;
import com.rydr.order.service.OrderService;

import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 *
 * @deprecated Retained as a reference implementation only. Production traffic uses
 * {@code GrabRedisRedissonServiceImpl} (single Redisson instance) for distributed locking.
 */
@Deprecated
@Slf4j
@Service("grabRedisRedissonRedLockLockService")
public class GrabRedisRedissonRedLockLockServiceImpl implements GrabService {

    // This module exposes four RedissonClient beans. Bind each field explicitly so that RedLock
    // really spans three independent Redis instances instead of relying on by-name fallback.
    // Do not mark one of them @Primary: primary wins over by-name matching and would silently
    // point all three fields at the same client.
    @Autowired
    @Qualifier("redissonRed1")
    private RedissonClient redissonRed1;
    @Autowired
    @Qualifier("redissonRed2")
    private RedissonClient redissonRed2;
    @Autowired
    @Qualifier("redissonRed3")
    private RedissonClient redissonRed3;

    @Autowired
	OrderService orderService;

    @Override
    public ResponseResult grabOrder(int orderId , int driverId){
        //generate key
        String lockKey = (RedisKeyConstant.GRAB_LOCK_ORDER_KEY_PRE + orderId).intern();
        //redisson lock - sentinel
//        RLock rLock = redisson.getLock(lockKey);
//        rLock.lock();

        //redisson lock - single node
//        RLock rLock = redissonRed1.getLock(lockKey);

        //red lock
        RLock rLock1 = redissonRed1.getLock(lockKey);
        RLock rLock2 = redissonRed2.getLock(lockKey);
        RLock rLock3 = redissonRed3.getLock(lockKey);
        RedissonRedLock rLock = new RedissonRedLock(rLock1,rLock2,rLock3);

        rLock.lock();

        try {
    		// This code by default sets key timeout to 30 seconds, and renews after 10 seconds
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
        	rLock.unlock();
        }
    }
}
