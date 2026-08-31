package com.rydr.driver.request;

import lombok.Data;

/**
 * Request body for POST /verify-code/verify
 *
 * @author oi
 */
@Data
public class CodeVerifyRequest {

	private int identity;

	private String phoneNumber;

	private String code;
}
