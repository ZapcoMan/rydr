package com.rydr.passenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;

import com.rydr.passenger.annotation.ExcudeFeignConfig;


/**
 * @author oi
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
@ComponentScan(
        basePackages = {"com.rydr"},
        excludeFilters = {
		    @ComponentScan.Filter(type = FilterType.ANNOTATION,value=ExcudeFeignConfig.class)
        }
)
public class ApiPassengerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiPassengerApplication.class, args);
	}
	
	
	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
}
