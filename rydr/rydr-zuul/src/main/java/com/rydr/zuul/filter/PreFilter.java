package com.rydr.zuul.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Pre filter (Spring Cloud Gateway version of the previous Zuul filter).
 * Only logs request information, never blocks the request.
 *
 * @author oi
 */
@Component
public class PreFilter implements GlobalFilter, Ordered {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		System.out.println("pre source uri: " + request.getURI().getPath());
		System.out.println("pre business logic token: " + request.getHeaders().getFirst("token"));
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return 5;
	}

}
