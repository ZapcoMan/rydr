package com.rydr.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author oi
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableAsync
@EnableJms
@EnableScheduling
// Registers the MyBatis mapper interfaces; without this the injected
// OrderMapper / OrderLockMapper would not resolve to any bean at startup.
@MapperScan("com.rydr.order.dao")
public class ServiceOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceOrderApplication.class, args);
	}


}
