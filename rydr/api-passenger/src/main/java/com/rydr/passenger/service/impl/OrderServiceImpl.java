package com.rydr.passenger.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rydr.common.dto.order.ForecastRequest;
import com.rydr.common.dto.order.ForecastResponse;
import com.rydr.dto.ResponseResult;
import com.rydr.entity.Order;
import com.rydr.passenger.feign.ServiceForecast;
import com.rydr.passenger.feign.ServiceOrderClient;
import com.rydr.passenger.service.OrderService;

import java.util.List;
import java.util.Map;

/**
 *
 * @author oi
 *
 */
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ServiceForecast serviceForecast;

	@Autowired
	private ServiceOrderClient serviceOrderClient;

	@Override
	public Double forecast(ForecastRequest forecastRequest) {
		ResponseResult<ForecastResponse> result = serviceForecast.forecast(forecastRequest);
		if (result == null || result.getCode() != com.rydr.constatnt.BusinessInterfaceStatus.SUCCESS.getCode()
				|| result.getData() == null) {
			return null;
		}
		return result.getData().getPrice();
	}

	@Override
	public Order createOrder(Order order) {
		ResponseResult<Order> result = serviceOrderClient.create(order);
		if (result == null || result.getCode() != com.rydr.constatnt.BusinessInterfaceStatus.SUCCESS.getCode()
				|| result.getData() == null) {
			return null;
		}
		return result.getData();
	}

	@Override
	public Order getByOrderNumber(String orderNumber) {
		ResponseResult<Order> result = serviceOrderClient.getByNumber(orderNumber);
		if (result == null || result.getData() == null) {
			return null;
		}
		return result.getData();
	}

	@Override
	public List<Order> listByPassenger(int passengerInfoId) {
		ResponseResult<List<Order>> result = serviceOrderClient.listByPassenger(passengerInfoId);
		if (result == null || result.getData() == null) {
			return null;
		}
		return result.getData();
	}

	@Override
	public Map<String, Object> pay(int orderId, int payType) {
		ResponseResult<Map<String, Object>> result = serviceOrderClient.pay(orderId, payType);
		if (result == null || result.getData() == null) {
			return null;
		}
		return result.getData();
	}

	@Override
	public Boolean cancel(int orderId, int cancelType) {
		ResponseResult<Boolean> result = serviceOrderClient.cancel(orderId, cancelType);
		if (result == null || result.getData() == null) {
			return false;
		}
		return result.getData();
	}

}
