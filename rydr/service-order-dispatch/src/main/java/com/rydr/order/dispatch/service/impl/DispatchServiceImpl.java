package com.rydr.order.dispatch.service.impl;

import com.rydr.common.constant.RedisKeyConstant;
import com.rydr.dto.ResponseResult;
import com.rydr.order.dispatch.service.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

	@Override
	public ResponseResult<List<Integer>> selectDrivers(int orderId, double userLng, double userLat, int maxDrivers) {
		Set<String> onlineDrivers;
		try {
			onlineDrivers = redisTemplate.opsForSet().members(RedisKeyConstant.DRIVER_ONLINE_SET);
		} catch (Exception e) {
			log.error("Failed to read online drivers for order {}", orderId, e);
			return ResponseResult.fail(com.rydr.constatnt.BusinessInterfaceStatus.FAIL.getCode(),
					"No online drivers available");
		}

		if (onlineDrivers == null || onlineDrivers.isEmpty()) {
			log.warn("No online driver for order {}", orderId);
			return ResponseResult.fail(com.rydr.constatnt.BusinessInterfaceStatus.FAIL.getCode(),
					"No online drivers available");
		}

		List<Candidate> candidates = new ArrayList<>();
		for (String driverIdStr : onlineDrivers) {
			int driverId;
			try {
				driverId = Integer.parseInt(driverIdStr);
			} catch (NumberFormatException ignored) {
				continue;
			}

			String loc = redisTemplate.opsForValue().get(RedisKeyConstant.DRIVER_LOCATION_PRE + driverId);
			if (loc == null || loc.isBlank()) {
				continue; // driver has no reported location, skip
			}
			String[] parts = loc.split(",");
			if (parts.length != 2) {
				continue;
			}
			double driverLng;
			double driverLat;
			try {
				driverLng = Double.parseDouble(parts[0].trim());
				driverLat = Double.parseDouble(parts[1].trim());
			} catch (NumberFormatException ignored) {
				continue;
			}

			double distance = haversine(userLng, userLat, driverLng, driverLat);
			double rate = 5.0;
			String rateStr = redisTemplate.opsForValue().get(RedisKeyConstant.DRIVER_RATE_PRE + driverId);
			if (rateStr != null && !rateStr.isBlank()) {
				try {
					rate = Double.parseDouble(rateStr.trim());
				} catch (NumberFormatException ignored) {
					// keep default
				}
			}
			candidates.add(new Candidate(driverId, distance, rate));
		}

		if (candidates.isEmpty()) {
			return ResponseResult.fail(com.rydr.constatnt.BusinessInterfaceStatus.FAIL.getCode(),
					"No located online drivers available");
		}

		// Rank: closer first, then higher rating.
		candidates.sort((a, b) -> {
			int cmp = Double.compare(a.distance, b.distance);
			if (cmp != 0) {
				return cmp;
			}
			return Double.compare(b.rate, a.rate);
		});

		List<Integer> result = candidates.stream()
				.limit(maxDrivers <= 0 ? 3 : maxDrivers)
				.map(c -> c.driverId)
				.collect(Collectors.toList());

		log.info("Selected drivers {} for order {}", result, orderId);
		return ResponseResult.success(result);
	}

	/**
	 * Great-circle distance in kilometres between two coordinates.
	 */
	private double haversine(double lng1, double lat1, double lng2, double lat2) {
		final int R = 6371;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	private static final class Candidate {
		final int driverId;
		final double distance;
		final double rate;

		Candidate(int driverId, double distance, double rate) {
			this.driverId = driverId;
			this.distance = distance;
			this.rate = rate;
		}
	}
}
