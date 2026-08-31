package com.rydr.driver.controller;

import com.rydr.common.constant.RedisKeyConstant;
import com.rydr.dto.ResponseResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Driver online / offline and location reporting, backing the real dispatch strategy.
 *
 * @author oi
 */
@RestController
@RequestMapping("/driver")
public class DriverStatusController {

	private static final long ONLINE_TTL_MINUTES = 30;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@PostMapping("/online")
	public ResponseResult<String> online(@RequestParam("driverId") int driverId,
										 @RequestParam(value = "rate", defaultValue = "5.0") double rate) {
		redisTemplate.opsForSet().add(RedisKeyConstant.DRIVER_ONLINE_SET, String.valueOf(driverId));
		redisTemplate.opsForValue().set(RedisKeyConstant.DRIVER_RATE_PRE + driverId,
				String.valueOf(rate), ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
		return ResponseResult.success("driver " + driverId + " is online");
	}

	@PostMapping("/offline")
	public ResponseResult<String> offline(@RequestParam("driverId") int driverId) {
		redisTemplate.opsForSet().remove(RedisKeyConstant.DRIVER_ONLINE_SET, String.valueOf(driverId));
		redisTemplate.delete(RedisKeyConstant.DRIVER_LOCATION_PRE + driverId);
		return ResponseResult.success("driver " + driverId + " is offline");
	}

	@PostMapping("/location")
	public ResponseResult<String> location(@RequestParam("driverId") int driverId,
										   @RequestParam("longitude") double longitude,
										   @RequestParam("latitude") double latitude) {
		redisTemplate.opsForSet().add(RedisKeyConstant.DRIVER_ONLINE_SET, String.valueOf(driverId));
		redisTemplate.opsForValue().set(RedisKeyConstant.DRIVER_LOCATION_PRE + driverId,
				longitude + "," + latitude, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
		return ResponseResult.success("location updated");
	}
}
