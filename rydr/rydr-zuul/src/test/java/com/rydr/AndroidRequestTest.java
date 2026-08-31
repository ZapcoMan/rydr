package com.rydr;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class AndroidRequestTest {


	@Test
	public void userRequest() {

		// Must match RequestCheckFilter's ZUUL_SECRET (same property, same default),
		// otherwise the signature computed here can never be verified by the gateway.
		String secret = System.getProperty("ZUUL_SECRET", "default-secret");

		RestTemplate userTemplate = new RestTemplate();

		// Timestamp
		String token = "test-token-value";
		Long timestamp = Calendar.getInstance().getTimeInMillis();

		String sign = DigestUtils.sha1Hex(token + timestamp + secret);


		// Set headers
		HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE,"application/json");
        httpHeaders.add("timestamp", timestamp+"");
        httpHeaders.add("sign", sign);
        httpHeaders.add("token", token);

        HttpEntity<String> request = new HttpEntity<>(httpHeaders);

		String result = userTemplate.postForEntity("http://localhost:9100/api-driver/test/hello?name=trp&age=70", request, String.class).getBody();
		System.out.println("Gateway response: "+result);

	}


}
