package com.rydr.order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.constatnt.OrderStatusEnum;
import com.rydr.dto.ResponseResult;
import com.rydr.entity.Order;
import com.rydr.order.config.ActiveMQTxConfig;
import com.rydr.order.dao.OrderMapper;
import com.rydr.order.dao.TxMessageMapper;
import com.rydr.order.entity.TxMessage;
import com.rydr.order.feign.WalletServiceClient;
import com.rydr.order.service.OrderService;
import com.rydr.common.util.JsonUtil;

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

	@Autowired
	private TxMessageMapper txMessageMapper;

	@Autowired(required = false)
	private JmsTemplate jmsTemplate;

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

		// Reliable-message outbox: write the payment command into tx_message in the
		// SAME local transaction as the order status update, then hand it to JMS.
		BigDecimal amount = orderAmount(order);
		String bizKey = "PAY_" + orderId;

		// Idempotency: if a command for this order is already in flight, do not enqueue twice.
		if (txMessageMapper.selectByBizKey(bizKey) != null) {
			result.put("success", false);
			result.put("code", BusinessInterfaceStatus.FAIL.getCode());
			result.put("message", "Payment command already in flight for order " + orderId);
			return result;
		}

		TxMessage msg = new TxMessage();
		msg.setBizKey(bizKey);
		msg.setTopic(ActiveMQTxConfig.QUEUE_PAY_DEDUCT);
		msg.setPayload(JsonUtil.toJson(Map.of(
				"orderId", orderId,
				"passengerId", order.getPassengerInfoId(),
				"amount", amount)));
		msg.setStatus(TxMessage.STATUS_INIT);
		msg.setRetry(0);
		msg.setNextRetryAt(null);
		txMessageMapper.insert(msg);

		// Send to ActiveMQ within the transaction; on failure the compensation job
		// re-delivers the still-INIT message, guaranteeing at-least-once delivery.
		sendPayCommand(msg);

		result.put("success", true);
		result.put("code", BusinessInterfaceStatus.SUCCESS.getCode());
		result.put("message", "Payment accepted; will be settled via reliable message");
		log.info("Order {} payment command enqueued, amount={}", orderId, amount);
		return result;
	}

	/**
	 * Deliver a payment command to ActiveMQ. On success the outbox row moves to SENT;
	 * on failure it stays INIT so {@link TxMessageCompensationJob} re-delivers later.
	 * At-least-once + wallet idempotency (biz_no) guarantee exactly-once effect.
	 */
	private void sendPayCommand(TxMessage msg) {
		try {
			jmsTemplate.convertAndSend(ActiveMQTxConfig.QUEUE_PAY_DEDUCT, msg.getPayload());
			txMessageMapper.updateStatus(msg.getId(), TxMessage.STATUS_SENT, msg.getRetry(), null);
			log.info("Payment command delivered for bizKey={}", msg.getBizKey());
		} catch (Exception e) {
			log.warn("JMS send failed for bizKey={}, will retry via compensation job: {}",
					msg.getBizKey(), e.getMessage());
		}
	}

	/**
	 * Called by the wallet-result consumer when the wallet service confirms a payment.
	 * Marks the order paid and the outbox message DONE.
	 */
	public void onPaySuccess(int orderId, int payType, String bizKey) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", orderId);
		params.put("status", OrderStatusEnum.STATUS_PAY_END.getCode());
		params.put("isPaid", 1);
		params.put("payType", payType);
		params.put("transactionId", "WALLET-" + orderId + "-" + System.currentTimeMillis());
		mapper.payOrder(params);
		if (bizKey != null) {
			TxMessage row = txMessageMapper.selectByBizKey(bizKey);
			if (row != null) {
				txMessageMapper.markDone(row.getId());
			}
		}
		log.info("Order {} confirmed paid via wallet", orderId);
	}

	/**
	 * Called by the wallet-result consumer when the wallet deduction failed.
	 * Rolls the order status back so the passenger can retry.
	 */
	public void onPayFailure(int orderId, String reason, String bizKey) {
		Order order = mapper.selectByPrimaryKey(orderId);
		if (order != null && order.getStatus() != null
				&& order.getStatus() == OrderStatusEnum.STATUS_PAY_START.getCode()) {
			order.setStatus(OrderStatusEnum.STATUS_DRIVER_TRAVEL_END.getCode());
			mapper.updateByPrimaryKeySelective(order);
		}
		if (bizKey != null) {
			txMessageMapper.deleteByBizKey(bizKey);
		}
		log.warn("Order {} payment failed, status rolled back: {}", orderId, reason);
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
	 * Resolve the fare for an order. The structured {@code fare_amount} column (settled
	 * at trip end) is preferred; it replaces the old fragile memo="fare=xxx" convention.
	 * Falls back to zero so the call never NPEs.
	 */
	private BigDecimal orderAmount(Order order) {
		if (order.getFareAmount() != null) {
			return order.getFareAmount();
		}
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
