package com.rydr.driver.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rydr.common.dto.order.ForecastRequest;
import com.rydr.common.dto.order.ForecastResponse;
import com.rydr.dto.ResponseResult;
import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.driver.feign.ServiceForecast;
import com.rydr.driver.feign.ServiceOrderClient;
import com.rydr.driver.service.OrderService;
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
		if (result == null || result.getCode() != BusinessInterfaceStatus.SUCCESS.getCode()
				|| result.getData() == null) {
			return null;
		}
		return result.getData().getPrice();
	}

	@Override
	public Boolean startTrip(int orderId) {
		ResponseResult<Boolean> result = serviceOrderClient.startTrip(orderId);
		if (result == null || result.getData() == null) {
			return false;
		}
		return result.getData();
	}

	@Override
	public Boolean endTrip(int orderId) {
		ResponseResult<Boolean> result = serviceOrderClient.endTrip(orderId);
		if (result == null || result.getData() == null) {
			return false;
		}
		return result.getData();
	}

}
