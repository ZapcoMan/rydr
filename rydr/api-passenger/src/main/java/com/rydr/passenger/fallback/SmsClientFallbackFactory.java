package com.rydr.passenger.fallback;

import com.rydr.common.dto.sms.SmsSendRequest;
import org.springframework.stereotype.Component;

import com.rydr.dto.ResponseResult;
import com.rydr.passenger.feign.SmsClient;

import org.springframework.cloud.openfeign.FallbackFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Graceful degradation for service-sms: report a normal failure response the caller can handle,
 * instead of letting the circuit breaker throw.
 *
 * @author oi
 */
@Component
@Slf4j
public class SmsClientFallbackFactory implements FallbackFactory<SmsClient> {

	@Override
	public SmsClient create(Throwable cause) {
		return new SmsClient() {

			@Override
			public ResponseResult sendSms(SmsSendRequest smsSendRequest) {
				log.error("SMS service unavailable, circuit breaker triggered: " + cause.getMessage(), cause);
				return ResponseResult.fail(-3, "SMS service unavailable, please retry later");
			}
		};
	}

}
