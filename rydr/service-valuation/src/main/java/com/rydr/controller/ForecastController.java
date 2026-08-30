package com.rydr.controller;

import com.rydr.dto.ResponseResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rydr.common.dto.order.ForecastRequest;
import com.rydr.common.dto.order.ForecastResponse;
import com.rydr.common.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author oi
 *
 */
@Slf4j
@RestController
@RequestMapping("/forecast")
public class ForecastController {

	@Value("${server.port}")
	String port;

	@PostMapping("/single")
	public ResponseResult forecast(@RequestBody ForecastRequest forecastRequest) {
		log.info("Valuation parameters: "+port+" "+JsonUtil.toJson(forecastRequest));

		ForecastResponse response = new ForecastResponse();

		// Calculate price based on start/end coordinates: base fare + per-km price * distance
		Double price = calculatePrice(forecastRequest);
		response.setPrice(price);
		return ResponseResult.success(response);

	}

	/**
	 * Rough valuation: base fare 10.0 yuan plus 2.0 yuan per km (straight-line distance via Haversine)
	 */
	private Double calculatePrice(ForecastRequest request) {
		double baseFare = 10.0;
		double perKmPrice = 2.0;
		double distanceKm = calculateDistanceKm(request);
		double price = baseFare + perKmPrice * distanceKm;
		// Round to 2 decimal places
		return Math.round(price * 100) / 100.0;
	}

	private double calculateDistanceKm(ForecastRequest request) {
		try {
			double startLat = Double.parseDouble(request.getStartLatitude());
			double startLon = Double.parseDouble(request.getStartLongitude());
			double endLat = Double.parseDouble(request.getEndLatitude());
			double endLon = Double.parseDouble(request.getEndLongitude());
			return distance(startLat, startLon, endLat, endLon);
		} catch (NumberFormatException e) {
			log.warn("Invalid coordinates, fallback to 0 distance");
			return 0.0;
		}
	}

	/**
	 * Haversine formula, result in km
	 */
	private double distance(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadius * c;
	}

}
