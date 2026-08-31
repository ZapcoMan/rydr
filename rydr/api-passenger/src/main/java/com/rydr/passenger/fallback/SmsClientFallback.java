package com.rydr.passenger.fallback;

import com.rydr.common.dto.sms.SmsSendRequest;
import org.springframework.stereotype.Component;

import com.rydr.dto.ResponseResult;
import com.rydr.passenger.feign.SmsClient;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 */
@Component
@Slf4j
public class SmsClientFallback implements SmsClient {

//	@Autowired
//	private StringRedisTemplate redisTemplate;

	@Override
	public ResponseResult sendSms(SmsSendRequest smsSendRequest) throws Exception{
		log.warn("Sorry, circuit breaker triggered for the SMS service");

//		String key = "service-sms";
//		String noticeString = redisTemplate.opsForValue().get(key);
//		if(StringUtils.isBlank(noticeString)) {
//			Send SMS or call phone notification API
//			System.out.println("Notify others that circuit breaker triggered");
//			redisTemplate.opsForValue().set(key, "notice", 30, TimeUnit.SECONDS);
//
//		}else {
//			System.out.println("Already notified, skipping notification for now");
//		}
		// Degrade gracefully: throwing out of a circuit breaker makes the failure unhandleable
		// for the caller, which is exactly what a fallback is supposed to prevent.
		return ResponseResult.fail(-3, "feign circuit breaker");
	}

}
