package com.rydr.service.impl;

import com.rydr.dto.ResponseResult;
import com.rydr.dao.entity.PassengerUserInfo;
import com.rydr.dao.mapper.PassengerUserInfoCustomMapper;
import com.rydr.service.ServicePassengerUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ServicePassengerUserInfoServiceImpl implements ServicePassengerUserInfoService {

    @Autowired
    private PassengerUserInfoCustomMapper passengerInfoMapperCustom;

    @Override
    public ResponseResult<PassengerUserInfo> login(String  passengerPhone) {
        // Query user info by phone number
        PassengerUserInfo passengerUserInfo = passengerInfoMapperCustom.selectByPhoneNumber(passengerPhone);
        if (passengerUserInfo == null){
            /*
             * Two concurrent first-time logins both see "no such user" and both try to insert.
             * Insert first and, if another instance won the race, simply read the row it created.
             * This relies on a unique index on passenger_phone; without it the duplicate would
             * be inserted silently instead of raising DuplicateKeyException.
             */
            PassengerUserInfo newUser = new PassengerUserInfo();
            newUser.setPassengerPhone(passengerPhone);
            newUser.setRegisterDate(new Date());

            try {
                passengerInfoMapperCustom.insertSelective(newUser);
                passengerUserInfo = newUser;
            } catch (DuplicateKeyException e) {
                log.info("Concurrent registration for {}, re-reading the existing user", passengerPhone);
                passengerUserInfo = passengerInfoMapperCustom.selectByPhoneNumber(passengerPhone);
                if (passengerUserInfo == null) {
                    // The duplicate came from something else than a concurrent registration
                    throw e;
                }
            }
        }

        // Record login time

        return ResponseResult.success(passengerUserInfo);
    }


}
