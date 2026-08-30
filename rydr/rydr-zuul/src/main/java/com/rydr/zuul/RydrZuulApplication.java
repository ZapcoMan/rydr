package com.rydr.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Gateway application.
 * Migrated from Netflix Zuul (@EnableZuulProxy) to Spring Cloud Gateway.
 *
 * @author oi
 */
@SpringBootApplication
@EnableDiscoveryClient
public class RydrZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(RydrZuulApplication.class, args);
	}

}
