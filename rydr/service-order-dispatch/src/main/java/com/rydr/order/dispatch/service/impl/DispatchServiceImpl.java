package com.rydr.order.dispatch.service.impl;

import com.rydr.common.constant.RedisKeyConstant;
import com.rydr.dto.ResponseResult;
import com.rydr.order.dispatch.service.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 */
@Service
@Slf4j
public class DispatchServiceImpl implements DispatchService {

	/**
	 * A pending offer has to expire: without a TTL the key stays in Redis forever whenever
	 * no driver ever picks the order up.
	 */
	@Value("${dispatch.order-ttl-minutes:30}")
	private long orderTtlMinutes;

	@Autowired
	private RedisTemplate<String , String> redisTemplate;

	@Override
	public ResponseResult dispatch(int orderId, List<Integer> driverIdList) {
		if (driverIdList == null || driverIdList.isEmpty()) {
			log.warn("No driver to dispatch order {} to", orderId);
			return ResponseResult.success("");
		}

		for (int driverId : driverIdList) {
			Boolean absent = redisTemplate.opsForValue().setIfAbsent(
					RedisKeyConstant.DRIVER_LISTEN_ORDER_PRE + driverId,
					orderId + "",
					orderTtlMinutes,
					TimeUnit.MINUTES);

			if (Boolean.FALSE.equals(absent)) {
				log.info("Driver {} still has a pending order, skipped order {}", driverId, orderId);
			}
		}

		return ResponseResult.success("");
	}
}
