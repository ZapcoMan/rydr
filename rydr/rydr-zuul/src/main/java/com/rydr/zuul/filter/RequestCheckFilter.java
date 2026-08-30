package com.rydr.zuul.filter;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Request legitimacy check (Spring Cloud Gateway version of the previous Zuul filter).
 *
 * @author oi
 */
@Component
public class RequestCheckFilter implements GlobalFilter, Ordered {

	@Value("${ZUUL_SECRET:default-secret}")
	private String secret;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String timestampStr = headers.getFirst("timestamp");
		String token = headers.getFirst("token");
		String sign = headers.getFirst("sign");

		// Only requests carrying signature headers are validated; others are passed through
		if (StringUtils.isBlank(timestampStr) && StringUtils.isBlank(sign)) {
			return chain.filter(exchange);
		}

		System.out.println("request check intercepted");

		boolean flag = true;
		Long timestamp = null;
		try {
			timestamp = Long.valueOf(timestampStr);
		} catch (NumberFormatException e) {
			flag = false;
		}

		String localSign = DigestUtils.sha1Hex(token + timestamp + secret);
		// Check if the timestamp is within 1 second
		long now = System.currentTimeMillis();
		if (flag && !(now - timestamp < 1 * 1000)) {
			flag = false;
		}
		if (flag && !localSign.trim().equals(sign)) {
			flag = false;
		}

		if (flag) {
			System.out.println("Request is legitimate");
			return chain.filter(exchange);
		}

		// Illegal request: reject it
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		DataBuffer buffer = response.bufferFactory()
				.wrap("Illegal request".getBytes(StandardCharsets.UTF_8));
		System.out.println("Request is illegal");
		return response.writeWith(Mono.just(buffer));
	}

	/**
	 * The smaller the value, the higher the priority
	 */
	@Override
	public int getOrder() {
		return 4;
	}

}
