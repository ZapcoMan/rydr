package com.rydr.passenger.service.impl;

import com.rydr.common.constant.IdentityConstant;
import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import com.rydr.common.dto.passengeruser.LoginRequest;
import com.rydr.common.util.JwtUtil;
import com.rydr.passenger.feign.ServicePassengerUserFeignClient;
import com.rydr.passenger.feign.ServiceVerificationCodeFeignClient;
import com.rydr.passenger.feign.request.CodeVerifyRequest;
import com.rydr.passenger.feign.response.PassengerUserInfo;
import com.rydr.passenger.response.UserAuthResponse;
import com.rydr.passenger.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private ServiceVerificationCodeFeignClient serviceVerificationCodeFeignClient;

    @Autowired
    private ServicePassengerUserFeignClient servicePassengerUserFeignClient;

    @Override
    public ResponseResult auth(String passengerPhone, String code) {
        // Verify the verification code
        CodeVerifyRequest codeVerifyRequest = new CodeVerifyRequest();
        codeVerifyRequest.setPhoneNumber(passengerPhone);
        codeVerifyRequest.setCode(code);
        codeVerifyRequest.setIdentity(IdentityConstant.PASSENGER);

        ResponseResult responseResult = serviceVerificationCodeFeignClient.verify(codeVerifyRequest);
        // ResponseResult uses BusinessInterfaceStatus: SUCCESS=0 / FAIL=1.
        // Both service-verification-code#verify and service-passenger-user#login return
        // ResponseResult.success(...) on success, i.e. code == 0. Never compare against 1 here.
        if(responseResult == null || responseResult.getCode() != BusinessInterfaceStatus.SUCCESS.getCode()){
            return ResponseResult.fail(
                    responseResult == null ? BusinessInterfaceStatus.FAIL.getCode() : responseResult.getCode(),
                    responseResult == null ? "Verification code service returned no response" : responseResult.getMessage());
        }

        // Passenger user login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassengerPhone(passengerPhone);
        ResponseResult<PassengerUserInfo> passengerUserInfoResponseResult = servicePassengerUserFeignClient.login(loginRequest);
        if(passengerUserInfoResponseResult == null
                || passengerUserInfoResponseResult.getCode() != BusinessInterfaceStatus.SUCCESS.getCode()){
            return ResponseResult.fail(
                    passengerUserInfoResponseResult == null ? BusinessInterfaceStatus.FAIL.getCode() : passengerUserInfoResponseResult.getCode(),
                    passengerUserInfoResponseResult == null ? "Passenger user service returned no response" : passengerUserInfoResponseResult.getMessage());
        }
        PassengerUserInfo passengerUserInfo = passengerUserInfoResponseResult.getData();
        if(passengerUserInfo == null){
            return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "Passenger user not found");
        }

        // Distribute token
        long passengerId = passengerUserInfo.getId();
        // Generate token via JWT; all endpoints requiring login authentication need to include the token and signature rules
        String subject = IdentityConstant.PASSENGER +"_"+passengerPhone+"_"+passengerId;
        String token = JwtUtil.createToken(subject,new Date());
        UserAuthResponse response = new UserAuthResponse();

        response.setToken(token);
        return ResponseResult.success(response);
    }
}
