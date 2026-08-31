package com.rydr.order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.constatnt.OrderStatusEnum;
import com.rydr.dto.ResponseResult;
import com.rydr.entity.Order;
import com.rydr.order.dao.OrderMapper;
import com.rydr.order.feign.WalletServiceClient;
import com.rydr.order.service.OrderService;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderMapper mapper;

	@Autowired(required = false)
	private WalletServiceClient walletServiceClient;

	@Override
	@Transactional
	public boolean grab(int orderId, int driverId) {
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order == null) {
			log.warn("Order {} does not exist, driver {} cannot grab it", orderId, driverId);
			return false;
		}

		if (order.getStatus() != null
				&& order.getStatus().intValue() == OrderStatusEnum.CALL_ORDER_FORECAST.getCode()
				&& order.getDriverId() == null) {
			order.setStatus(OrderStatusEnum.STATUS_DRIVER_ACCEPT.getCode());
			order.setDriverId(driverId);
			order.setDriverStatus(1);
			order.setDriverGrabTime(new Date());
			mapper.updateByPrimaryKeySelective(order);
			log.info("Driver {} grabbed order {}", driverId, orderId);
			return true;
		}

		log.info("Order {} already taken or not dispatchable, driver {} cannot grab it",
				orderId, driverId);
		return false;
	}

	@Override
	@Transactional
	public Order createOrder(Order order) {
		if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
			order.setOrderNumber(generateOrderNumber());
		}
		if (order.getStatus() == null) {
			order.setStatus(OrderStatusEnum.CALL_ORDER_FORECAST.getCode());
		}
		order.setCreateTime(new Date());
		order.setStartTime(new Date());
		mapper.insertSelective(order);
		log.info("Created order {} for passenger {}", order.getId(), order.getPassengerInfoId());
		return order;
	}

	@Override
	public Order getByOrderNumber(String orderNumber) {
		return mapper.selectByOrderNumber(orderNumber);
	}

	@Override
	public List<Order> listByPassenger(int passengerInfoId) {
		return mapper.selectByPassenger(passengerInfoId);
	}

	@Override
	@Transactional
	public boolean startTrip(int orderId) {
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order == null
				|| order.getStatus() == null
				|| order.getStatus() != OrderStatusEnum.STATUS_DRIVER_ACCEPT.getCode()) {
			log.warn("Order {} is not in an acceptable state to start the trip", orderId);
			return false;
		}
		order.setStatus(OrderStatusEnum.STATUS_DRIVER_TRAVEL_START.getCode());
		order.setDriverStatus(4);
		order.setReceivePassengerTime(new Date());
		mapper.updateByPrimaryKeySelective(order);
		log.info("Trip started for order {}", orderId);
		return true;
	}

	@Override
	@Transactional
	public boolean endTrip(int orderId) {
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order == null
				|| order.getStatus() == null
				|| order.getStatus() != OrderStatusEnum.STATUS_DRIVER_TRAVEL_START.getCode()) {
			log.warn("Order {} is not in an acceptable state to end the trip", orderId);
			return false;
		}
		order.setStatus(OrderStatusEnum.STATUS_DRIVER_TRAVEL_END.getCode());
		order.setDriverStatus(6);
		order.setPassengerGetoffTime(new Date());
		mapper.updateByPrimaryKeySelective(order);
		log.info("Trip ended for order {}", orderId);
		return true;
	}

	@Override
	@Transactional
	public Map<String, Object> pay(int orderId, int payType) {
		Map<String, Object> result = new HashMap<>();
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order == null
				|| order.getStatus() == null
				|| order.getStatus() != OrderStatusEnum.STATUS_DRIVER_TRAVEL_END.getCode()) {
			result.put("success", false);
			result.put("code", BusinessInterfaceStatus.FAIL.getCode());
			result.put("message", "Order is not ready for payment");
			return result;
		}

		// Mark payment initiated first so a retry cannot double-charge.
		order.setStatus(OrderStatusEnum.STATUS_PAY_START.getCode());
		mapper.updateByPrimaryKeySelective(order);

		if (walletServiceClient == null) {
			result.put("success", false);
			result.put("code", BusinessInterfaceStatus.FAIL.getCode());
			result.put("message", "Wallet service is unavailable");
			return result;
		}

		try {
			BigDecimal amount = orderAmount(order);
			ResponseResult<Void> payResult = walletServiceClient.pay(
					order.getPassengerInfoId(), orderId, amount);
			if (payResult != null && payResult.getCode() == BusinessInterfaceStatus.SUCCESS.getCode()) {
				Map<String, Object> params = new HashMap<>();
				params.put("id", orderId);
				params.put("status", OrderStatusEnum.STATUS_PAY_END.getCode());
				params.put("isPaid", 1);
				params.put("payType", payType);
				params.put("transactionId", "WALLET-" + orderId + "-" + System.currentTimeMillis());
				mapper.payOrder(params);
				result.put("success", true);
				result.put("code", BusinessInterfaceStatus.SUCCESS.getCode());
				result.put("message", "Paid successfully");
				log.info("Order {} paid successfully, amount={}", orderId, amount);
			} else {
				// rollback payment initiated state
				order.setStatus(OrderStatusEnum.STATUS_DRIVER_TRAVEL_END.getCode());
				mapper.updateByPrimaryKeySelective(order);
				result.put("success", false);
				result.put("code", BusinessInterfaceStatus.FAIL.getCode());
				result.put("message", payResult == null ? "Wallet returned null" : payResult.getMessage());
				log.warn("Order {} payment failed via wallet", orderId);
			}
		} catch (Exception e) {
			order.setStatus(OrderStatusEnum.STATUS_DRIVER_TRAVEL_END.getCode());
			mapper.updateByPrimaryKeySelective(order);
			result.put("success", false);
			result.put("code", BusinessInterfaceStatus.FAIL.getCode());
			result.put("message", "Payment error: " + e.getMessage());
			log.error("Order {} payment error", orderId, e);
		}
		return result;
	}

	@Override
	@Transactional
	public boolean cancel(int orderId, int cancelOrderType) {
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order == null) {
			return false;
		}
		if (order.getStatus() != null
				&& (order.getStatus() == OrderStatusEnum.STATUS_PAY_END.getCode()
				|| order.getStatus() == OrderStatusEnum.STATUS_PAY_START.getCode())) {
			log.warn("Order {} already paid, cannot cancel", orderId);
			return false;
		}
		Map<String, Object> params = new HashMap<>();
		params.put("id", orderId);
		params.put("status", 9);
		params.put("isCancel", 1);
		params.put("cancelOrderType", cancelOrderType);
		mapper.cancelOrder(params);
		log.info("Order {} cancelled, type={}", orderId, cancelOrderType);
		return true;
	}

	/**
	 * Resolve the fare for an order. The valuation service computes the price at forecast
	 * time; for now the amount is carried on the order memo as "fare=xxx" that the
	 * dispatch / trip-end flow populates. Falls back to zero so the call never NPEs.
	 */
	private BigDecimal orderAmount(Order order) {
		if (order.getMemo() != null && order.getMemo().startsWith("fare=")) {
			try {
				return new BigDecimal(order.getMemo().substring("fare=".length()).trim());
			} catch (NumberFormatException ignored) {
				log.warn("Invalid fare memo on order {}, defaulting to zero", order.getId());
			}
		}
		return BigDecimal.ZERO;
	}

	private String generateOrderNumber() {
		return "RYDR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
	}
}
