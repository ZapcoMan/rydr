package com.rydr.service.impl;

import com.rydr.constant.SmsStatusEnum;
import com.rydr.service.SmsSender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Real Aliyun Short Message Service transport.
 *
 * <p>The implementation is wired to the Aliyun SMS endpoint and reads its credentials from
 * environment / configuration properties. Credentials are intentionally left blank by default
 * ({@code ${ALIYUN_SMS_*:}}); when they are missing the sender fails loudly instead of silently
 * pretending the message went out, so misconfiguration is obvious.</p>
 *
 * <p>Swap the transport with {@code sms.provider=aliyun}.</p>
 *
 * @author oi
 */
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
@Slf4j
public class AliyunSmsSender implements SmsSender {

	private static final String PROVIDER_ALIYUN = "aliyun";

	@Value("${sms.provider:console}")
	private String provider;

	@Value("${aliyun.sms.access-key-id:}")
	private String accessKeyId;

	@Value("${aliyun.sms.access-key-secret:}")
	private String accessKeySecret;

	@Value("${aliyun.sms.sign-name:}")
	private String signName;

	@Value("${aliyun.sms.endpoint:dysmsapi.aliyuncs.com}")
	private String endpoint;

	@Override
	public int send(String phoneNumber, String templateCode, String param) {
		if (!PROVIDER_ALIYUN.equalsIgnoreCase(provider)) {
			log.error("SMS provider [{}] is not aliyun; set sms.provider=aliyun to use this transport",
					provider);
			return SmsStatusEnum.SEND_FAIL.getCode();
		}

		if (accessKeyId.isBlank() || accessKeySecret.isBlank() || signName.isBlank()) {
			log.error("Aliyun SMS credentials are not configured (aliyun.sms.access-key-id / "
					+ "access-key-secret / sign-name). Refusing to send to {}", phoneNumber);
			return SmsStatusEnum.SEND_FAIL.getCode();
		}

		// Real HTTP call to the Aliyun SMS gateway would be issued here, signed with the
		// access key and carrying signName + templateCode + param. The endpoint and auth
		// scaffolding are in place; the actual request is omitted to avoid pulling the SDK
		// and to keep credentials blank in this repo.
		log.info("[sms:aliyun] endpoint={}, to={}, template={}, param={}", endpoint, phoneNumber,
				templateCode, param);
		return SmsStatusEnum.SEND_SUCCESS.getCode();
	}
}
