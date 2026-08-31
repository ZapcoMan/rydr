package com.rydr.passenger.service.impl;

import com.rydr.common.constant.IdentityConstant;
import com.rydr.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.common.dto.verificationcode.VerifyCodeResponse;
import com.rydr.passenger.feign.request.CodeVerifyRequest;
import com.rydr.passenger.service.VerificationCodeService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author oi
 */
@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

	@Autowired
	private RestTemplate restTemplate;
	
	private final String SERVICE_VERIFICATION_CODE_SERVICE = "service-verification-code";
	
	@Override
	public String getCode(String phoneNumber) {
		String url = "http://"+SERVICE_VERIFICATION_CODE_SERVICE+"/verify-code/generate/"+ IdentityConstant.PASSENGER+ "/" +phoneNumber;
		ResponseResult result = restTemplate.exchange(url, HttpMethod.GET,new HttpEntity<Object>(null,null),ResponseResult.class).getBody();

		if(result != null && result.getCode() == BusinessInterfaceStatus.SUCCESS.getCode()) {
			VerifyCodeResponse response = JsonUtil.toBean(result.getData(), VerifyCodeResponse.class);
			return response == null ? "" : response.getCode();
		}else {
			log.warn("Failed to generate verification code for passenger phone={}", phoneNumber);
			return "";
		}
	}

	/**
	 * Verify the verification code against service-verification-code.
	 *
	 * @return "1" when the code is valid, "0" when it is invalid or the call failed
	 */
	@Override
	public String checkCode(String phoneNumber, String code) {
		String url = "http://" + SERVICE_VERIFICATION_CODE_SERVICE + "/verify-code/verify";
		CodeVerifyRequest request = new CodeVerifyRequest();
		request.setIdentity(IdentityConstant.PASSENGER);
		request.setPhoneNumber(phoneNumber);
		request.setCode(code);

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			ResponseResult result = restTemplate.exchange(
					url, HttpMethod.POST, new HttpEntity<Object>(request, headers), ResponseResult.class).getBody();

			if (result != null && result.getCode() == BusinessInterfaceStatus.SUCCESS.getCode()) {
				return "1";
			}
			log.warn("Verification code mismatch for passenger phone={}, message={}",
					phoneNumber, result == null ? "no response" : result.getMessage());
		} catch (Exception e) {
			log.error("Failed to check verification code for passenger phone=" + phoneNumber, e);
		}
		return "0";
	}

}
