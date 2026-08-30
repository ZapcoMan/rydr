package com.rydr.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;

import com.rydr.driver.annotation.ExcudeRibbonConfig;

/**
 * @author oi
 */

@SpringBootApplication
// Scan excludes classes annotated with the specified annotation
@ComponentScan(
        basePackages = {"com.rydr"},
        excludeFilters = {
		    @ComponentScan.Filter(type = FilterType.ANNOTATION,value=ExcudeRibbonConfig.class)
        }
 )
// Ribbon client configuration removed: replaced by Spring Cloud LoadBalancer
public class ApiDriverApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiDriverApplication.class, args);
	}


	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	/**
	 * Simple manual ribbon implementation
	 * @return
	 */
//	@Bean
//	public RestTemplate restTemplate() {
//		return new RestTemplate();
//	}

}
