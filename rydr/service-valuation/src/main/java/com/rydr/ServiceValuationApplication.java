package com.rydr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
/**
 * @author oi
 */
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.rydr.mapper")
public class ServiceValuationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceValuationApplication.class, args);
	}

}
