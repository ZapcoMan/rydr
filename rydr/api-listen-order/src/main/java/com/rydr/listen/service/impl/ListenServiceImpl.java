package com.rydr.listen.service.impl;

import com.rydr.common.constant.RedisKeyConstant;
import com.rydr.listen.response.PreGrabResponse;
import com.rydr.listen.service.ListenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 */
@Service
@Slf4j
public class ListenServiceImpl implements ListenService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * Take the order currently assigned to the given driver.
     *
     * service-order-dispatch writes the order id to {@code driver_order_list_<driverId>}.
     * The key is read and removed atomically so that concurrent listeners cannot hand the
     * same order to the driver twice.
     *
     * @param driverId driver id
     * @return the pending order, or {@code null} when no order is waiting for this driver
     */
    @Override
    public PreGrabResponse listen(int driverId) {
        String key = RedisKeyConstant.DRIVER_LISTEN_ORDER_PRE + driverId;

        String orderId;
        try {
            orderId = redisTemplate.opsForValue().getAndDelete(key);
        } catch (Exception e) {
            log.error("Failed to read pending order from Redis, driverId=" + driverId, e);
            return null;
        }

        if (orderId == null || orderId.isBlank()) {
            return null;
        }

        try {
            PreGrabResponse response = new PreGrabResponse();
            response.setOrderId(Integer.parseInt(orderId.trim()));
            log.info("Picked up pending order {} for driver {}", response.getOrderId(), driverId);
            return response;
        } catch (NumberFormatException e) {
            log.error("Invalid order id [{}] stored in Redis for driverId={}", orderId, driverId);
            return null;
        }
    }
}
