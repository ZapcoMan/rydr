package com.rydr.rabbitmq;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Functional Spring Cloud Stream consumer (replaces the removed @EnableBinding / @StreamListener).
 * Bean name "input" maps to binding name "input-in-0".
 */
@Configuration
public class MyStreamListener {

	@Bean
	public Consumer<String> input() {
		return s -> System.out.println("Listening to message queue manual content: " + s);
	}
}
