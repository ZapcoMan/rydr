package com.rydr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
/**
 * @author oi
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ServicePassengerUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicePassengerUserApplication.class, args);
	}

}
