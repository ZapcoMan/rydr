package com.rydr.passenger.feign;

import com.rydr.common.dto.sms.SmsSendRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.rydr.dto.ResponseResult;
import com.rydr.passenger.fallback.SmsClientFallbackFactory;

/**
 * @author oi
 */


//@FeignClient(name = "service-sms",configuration = FeignDisableHystrixConfiguration.class)
//@FeignClient(name = "service-sms")
//@FeignClient(name = "service-sms",fallback = SmsClientFallback.class)
// FallbackFactory is preferred over fallback: it also exposes the failure cause, and it
// returns a normal failure response instead of throwing out of the circuit breaker.
@FeignClient(name = "service-sms",fallbackFactory = SmsClientFallbackFactory.class)
public interface SmsClient {
	/**
	 * Send verification code via SMS template
	 * @param smsSendRequest
	 * @return
	 */
	@RequestMapping(value="/send/alisms-template", method = RequestMethod.POST)
	public ResponseResult sendSms(@RequestBody SmsSendRequest smsSendRequest) throws Exception;
}
