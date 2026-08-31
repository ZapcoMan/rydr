package com.rydr.driver.service;

import com.rydr.common.dto.order.ForecastRequest;

/**
 *
 * @author oi
 *
 */
public interface OrderService {
	/**
	 * Calculate estimated price based on start and end coordinates
	 * @param forecastRequest
	 * @return
	 */
	public Double forecast(ForecastRequest forecastRequest);

	/**
	 * Driver starts the trip (passenger boarded).
	 */
	Boolean startTrip(int orderId);

	/**
	 * Driver ends the trip (passenger dropped off, unpaid).
	 */
	Boolean endTrip(int orderId);
}
