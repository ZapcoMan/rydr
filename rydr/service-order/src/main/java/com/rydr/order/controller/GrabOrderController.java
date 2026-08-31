package com.rydr.order.controller;

import com.rydr.dto.ResponseResult;
import com.rydr.order.service.GrabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * @author oi
 */
@RestController
@RequestMapping("/grab")
public class GrabOrderController {

    @Autowired
    // no lock
//    @Qualifier("grabNoLockService")
    // JVM lock
//    @Qualifier("grabJvmLockService")
    // MySQL lock
//    @Qualifier("grabMysqlLockService")
    // single Redisson
    @Qualifier("grabRedisRedissonService")
    // red lock
//    @Qualifier("grabRedisRedissonRedLockLockService")
    private GrabService grabService;


    @GetMapping("/do/{orderId}")
    public ResponseResult grab(@PathVariable("orderId") int orderId, int driverId){
        return grabService.grabOrder(orderId, driverId);
    }
}
