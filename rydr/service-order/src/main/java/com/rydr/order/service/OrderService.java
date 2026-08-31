package com.rydr.order.service;

import com.rydr.entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {

	/**
	 * Driver grabs an order. Real state-machine transition guarded by a distributed lock.
	 * @param orderId order id
	 * @param driverId driver id
	 * @return true when the grab succeeded
	 */
	public boolean grab(int orderId, int driverId);

	/**
	 * Create a new passenger order (status 0 - estimated / to-be-dispatched).
	 */
	Order createOrder(Order order);

	/**
	 * Load an order by its business order number.
	 */
	Order getByOrderNumber(String orderNumber);

	/**
	 * Load a passenger's most recent orders (for history / status query).
	 */
	List<Order> listByPassenger(int passengerInfoId);

	/**
	 * Driver arrives at the pickup point and starts the trip (status 5).
	 */
	boolean startTrip(int orderId);

	/**
	 * Trip finished, passenger dropped off (status 6 - unpaid).
	 */
	boolean endTrip(int orderId);

	/**
	 * Pay the order via the wallet service (status 6 -> 7 -> 8).
	 * @return a map with keys: success, code, message
	 */
	Map<String, Object> pay(int orderId, int payType);

	/**
	 * Cancel an order (status 9).
	 */
	boolean cancel(int orderId, int cancelOrderType);
}
