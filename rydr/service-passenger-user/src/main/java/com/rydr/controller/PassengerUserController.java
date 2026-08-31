package com.rydr.controller;

import com.rydr.dto.ResponseResult;
import com.rydr.dao.mapper.PassengerUserInfoCustomMapper;
import com.rydr.dao.entity.PassengerUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author oi
 */
@RestController
@RequestMapping("/passenger-user")
public class PassengerUserController {

    @Autowired
    private PassengerUserInfoCustomMapper passengerUserInfoMapperCustom;

    /**
     * Query passenger profile by phone number.
     */
    @GetMapping("/get-by-phone/{phoneNumber}")
    public ResponseResult<PassengerUserInfo> getByPhone(@PathVariable("phoneNumber") String phoneNumber) {
        PassengerUserInfo userInfo = passengerUserInfoMapperCustom.selectByPhoneNumber(phoneNumber);
        return ResponseResult.success(userInfo);
    }
}
