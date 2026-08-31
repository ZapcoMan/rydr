package com.rydr.driver.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.rydr.common.dto.sms.SmsSendRequest;
import com.rydr.common.dto.sms.SmsTemplateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.driver.service.RestTemplateRequestService;
import com.rydr.driver.service.ShortMsgService;

import com.rydr.common.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;
/**
 * @author oi
 */
@Service
@Slf4j
public class ShortMsgServiceImpl implements ShortMsgService {

	@Autowired
	private RestTemplateRequestService restTemplateRequestService;

	/**
	 * Template id registered in service-sms; override with -Dsms.template-id=...
	 */
	@Value("${sms.template-id:SMS_144145499}")
	private String smsTemplateId;

	@Override
	public ResponseResult send(String phoneNumber, String code) {

		log.info("Sending verification code SMS, phone={}", phoneNumber);

		if (phoneNumber == null || phoneNumber.isBlank() || code == null || code.isBlank()) {
			log.warn("Refusing to send SMS with blank phone number or code, phone={}", phoneNumber);
			return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(),
					"Phone number or verification code is empty");
		}

		SmsSendRequest smsSendRequest = new SmsSendRequest();
		String[] phoneNumbers = new String[] {phoneNumber};
		smsSendRequest.setReceivers(phoneNumbers);

		List<SmsTemplateDto> data = new ArrayList<SmsTemplateDto>();
		SmsTemplateDto dto = new SmsTemplateDto();
		dto.setId(smsTemplateId);
		HashMap<String, Object> templateMap = new HashMap<String, Object>(1);
		templateMap.put("code", code);
		dto.setTemplateMap(templateMap);
		data.add(dto);

		smsSendRequest.setData(data);

//		 Normal ribbon call
		ResponseResult result;
		try {
			result = restTemplateRequestService.smsSend(smsSendRequest);
		} catch (Exception e) {
			log.error("Failed to call SMS service, phone=" + phoneNumber, e);
			return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "SMS service unavailable");
		}

		log.info("Result returned from SMS service call: {}", JsonUtil.toJson(result));
		if (result == null) {
			return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(),
					"SMS service returned no response");
		}
		return result;
	}

	/*
	 *	Code below is a manual ribbon implementation
	 */

	@Autowired
	DiscoveryClient discoveryClient;

	private ServiceInstance loadBalance(String serviceName) {
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
		ServiceInstance instance = instances.get(new Random().nextInt(instances.size()));
		log.info("Load balancing selected IP: "+instance.getHost()+", port: "+instance.getPort());

		Map<String, String> metadata = instance.getMetadata();

		return instance;
	}

	/*
	 *	Code above is a manual ribbon implementation
	 */

}
