package com.rydr.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.jms.annotation.EnableJms;
/**
 * @author oi
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableJms
public class ServiceWalletApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceWalletApplication.class, args);
	}

}
