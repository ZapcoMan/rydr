package com.rydr.driver.exception;

/**
 * Business exception.
 * Previously extended HystrixBadRequestException; Hystrix was removed in Spring Cloud 2025.x,
 * so it now extends RuntimeException directly.
 */
public class BusinessException extends RuntimeException {

	private String message;

	public BusinessException(String message) {
		super(message);
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

}
