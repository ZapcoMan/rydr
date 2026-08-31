package com.rydr.zuul.filter;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
 * Global authentication filter: every request routed to an api-* service must carry a valid
 * JWT, except for a small whitelist (login, verification code, health, SSE streams, gateway
 * probes and actuator). A request without a valid token is rejected with 401 and is never
 * forwarded downstream.
 *
 * @author oi
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

	/** Paths that may be reached without a JWT. */
	private static final List<String> WHITELIST = List.of(
			"/api-passenger/auth",
			"/api-passenger/verification-code",
			"/api-passenger/order/forecast",
			"/api-driver/auth",
			"/api-driver/verification-code",
			"/api-listen-order",
			"/actuator",
			"/api-passenger/api-passenger-gateway-test");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String uri = request.getURI().getPath();

		if (isWhitelisted(uri)) {
			return chain.filter(exchange);
		}

		String token = resolveToken(request);
		String subject = token == null ? "" : JwtUtil.parseToken(token);

		if (StringUtils.isNotBlank(subject)) {
			// Propagate the authenticated subject downstream for audit / logging.
			ServerHttpRequest mutated = request.mutate()
					.header("X-Authenticated-Subject", subject)
					.build();
			return chain.filter(exchange.mutate().request(mutated).build());
		}

		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
		DataBuffer buffer = response.bufferFactory()
				.wrap("{\"code\":1,\"message\":\"Authentication failed\"}".getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}

	private boolean isWhitelisted(String uri) {
		if (uri == null) {
			return false;
		}
		for (String prefix : WHITELIST) {
			if (uri.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private String resolveToken(ServerHttpRequest request) {
		String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (StringUtils.isNotBlank(auth)) {
			if (auth.startsWith("Bearer ")) {
				return auth.substring("Bearer ".length()).trim();
			}
			return auth.trim();
		}
		return request.getHeaders().getFirst("token");
	}

	/**
	 * The smaller the value, the higher the priority. Runs right after the rate limiter.
	 */
	@Override
	public int getOrder() {
		return 4;
	}
}
