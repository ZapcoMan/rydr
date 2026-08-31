package com.rydr.passenger.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rydr.common.dto.sms.SmsSendRequest;
import com.rydr.common.dto.sms.SmsTemplateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.passenger.feign.SmsClient;
import com.rydr.passenger.service.ShortMsgService;

import com.rydr.common.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 */
@Service
@Slf4j
public class ShortMsgServiceImpl implements ShortMsgService {

	@Autowired
	private SmsClient smsClient;

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

		// Feign call to SERVICE-SMS
		ResponseResult result;
		try {
			result = smsClient.sendSms(smsSendRequest);
		} catch (Exception e) {
			// Degrade gracefully: return a normal failure response instead of propagating a 500
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

}
