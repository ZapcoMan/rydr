package com.rydr.zuul.filter;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.rydr.common.util.JwtUtil;

import reactor.core.publisher.Mono;

/**
 * Authentication filter (Spring Cloud Gateway version of the previous Zuul filter).
 *
 * @author oi
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

	/**
	 * Only this endpoint will require authentication
	 */
	private static final String CHECK_URI = "/api-passenger/api-passenger-gateway-test/hello";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String uri = request.getURI().getPath();
		System.out.println("Source uri: " + uri);

		// Only the configured endpoint is intercepted
		if (!CHECK_URI.equalsIgnoreCase(uri)) {
			return chain.filter(exchange);
		}

		System.out.println("auth intercepted");
		String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		String parseToken = JwtUtil.parseToken(token);

		if (StringUtils.isNotBlank(parseToken)) {
			System.out.println("auth filter: verification passed");
			return chain.filter(exchange);
		}

		// Authentication failed: stop the request instead of forwarding it downstream
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		DataBuffer buffer = response.bufferFactory()
				.wrap("Authentication failed".getBytes(StandardCharsets.UTF_8));
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
