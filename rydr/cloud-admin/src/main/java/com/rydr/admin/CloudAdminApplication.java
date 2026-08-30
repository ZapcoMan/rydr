package com.rydr.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import de.codecentric.boot.admin.server.config.EnableAdminServer;

/**
 * @author oi
 */
@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
public class CloudAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudAdminApplication.class, args);
	}

}
