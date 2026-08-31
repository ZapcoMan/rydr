package com.rydr.passenger.service;

import com.rydr.common.dto.order.ForecastRequest;
import com.rydr.entity.Order;

import java.util.List;
import java.util.Map;

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
	 * Create a new passenger order.
	 */
	Order createOrder(Order order);

	/**
	 * Query an order by its business order number.
	 */
	Order getByOrderNumber(String orderNumber);

	/**
	 * List a passenger's orders.
	 */
	List<Order> listByPassenger(int passengerInfoId);

	/**
	 * Pay an order.
	 */
	Map<String, Object> pay(int orderId, int payType);

	/**
	 * Cancel an order.
	 */
	Boolean cancel(int orderId, int cancelType);
}
