package com.rydr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

/**
 * DEPRECATED: Hystrix Dashboard was removed from Spring Cloud in the 2020.x release train
 * and does not exist in Spring Cloud 2025.x. This module is excluded from the parent
 * reactor build (see {@code rydr/pom.xml}) and is kept for historical reference only.
 *
 * <p>Replacement: use Resilience4j with Micrometer metrics exposed via Actuator,
 * then visualise them in Grafana (or any Micrometer-compatible dashboard).
 */
@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(HystrixDashboardApplication.class, args);
	}

}
