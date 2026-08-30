package com.rydr.common.dto.passengeruser;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @author oi
 */
@Data
public class LoginRequest {

    @NotBlank
    private String passengerPhone;

}
