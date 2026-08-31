package com.rydr.zuul.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.google.common.util.concurrent.RateLimiter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Rate limiting filter (Spring Cloud Gateway version of the previous Zuul filter).
 * Disabled by default; enable with rydr.gateway.rate-limit.enabled=true
 *
 * @author oi
 */
@Component
@Slf4j
public class RateFilter implements GlobalFilter, Ordered {

	/**
	 * If set to 5, it means 5 tokens per second; the actual value should be obtained through stress testing.
	 *
	 * 1. Creates a RateLimiter with a stable token output rate, ensuring no more than permitsPerSecond requests per second on average.
	 * 2. When request arrival rate exceeds permitsPerSecond, ensures only permitsPerSecond requests are processed per second.
	 * 3. When this RateLimiter is underutilized, it will accumulate up to permitsPerSecond tokens.
	 */
	private static final RateLimiter RATE_LIMITER = RateLimiter.create(5);

	@Value("${rydr.gateway.rate-limit.enabled:true}")
	private boolean rateLimitEnabled;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (!rateLimitEnabled) {
			return chain.filter(exchange);
		}

		/**
		 * Returns immediately if no token is available
		 */
		if (!RATE_LIMITER.tryAcquire()) {
			log.info("rate filter cannot acquire token, rate limited");
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			DataBuffer buffer = response.bufferFactory()
					.wrap("Too many requests".getBytes(StandardCharsets.UTF_8));
			return response.writeWith(Mono.just(buffer));
		}
		return chain.filter(exchange);
	}

	/**
	 * Rate limiting should have the highest priority
	 */
	@Override
	public int getOrder() {
		return -10;
	}

}
