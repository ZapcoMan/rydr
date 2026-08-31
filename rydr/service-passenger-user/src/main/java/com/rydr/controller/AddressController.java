package com.rydr.controller;

import com.rydr.dao.mapper.PassengerAddressMapper;
import com.rydr.dto.ResponseResult;
import com.rydr.entity.PassengerAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author oi
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private PassengerAddressMapper passengerAddressMapper;

    /**
     * Get address information by passenger ID and address type
     * @param passengerId
     * @param type
     * @return
     */
    @GetMapping("/get-address/{passengerId}/{type}")
    public ResponseResult getAddress(@PathVariable("passengerId") int passengerId, @PathVariable("type") int type){
        List<PassengerAddress> addresses = passengerAddressMapper.selectByPassengerAndType(passengerId, type);
        return ResponseResult.success(addresses);
    }
}
