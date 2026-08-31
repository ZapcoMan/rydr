package com.rydr.driver.service.impl;

import com.rydr.common.dto.sms.SmsSendRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.rydr.dto.ResponseResult;
import com.rydr.driver.constant.HttpUrlConstants;
import com.rydr.driver.service.RestTemplateRequestService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RestTemplateRequestServiceImpl implements RestTemplateRequestService {

	@Autowired
	private RestTemplate restTemplate;

	@Override
	public ResponseResult smsSend(SmsSendRequest smsSendRequest) {
		String url = HttpUrlConstants.SERVICE_SMS_URL + "/send/alisms-template";
		return restTemplate.postForEntity(url, smsSendRequest, ResponseResult.class).getBody();
	}

	public String grabOrder(int orderId, int driverId) {

		String url = HttpUrlConstants.SERVICE_ORDER_URL + "/grab/do/"+orderId+"?driverId="+driverId;

		return restTemplate.getForEntity(url, String.class).getBody();
	}

}
