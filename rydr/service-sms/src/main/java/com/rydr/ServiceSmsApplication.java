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
// Registers the MyBatis mapper interfaces; without this the injected
// ServiceSmsRecordMapper / ServiceSmsTemplateMapper would not resolve at startup.
@MapperScan("com.rydr.dao.mapper")
public class ServiceSmsApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(ServiceSmsApplication.class, args);
	}

}
