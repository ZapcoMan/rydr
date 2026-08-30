package com.rydr.listen.controller;

import com.rydr.common.util.JsonUtil;
import com.rydr.listen.response.PreGrabResponse;
import com.rydr.listen.service.ListenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author oi
 */
@RestController
@RequestMapping("/listen")
public class ListenController {

    @Autowired
    private ListenService listenService;

    @RequestMapping(value = "/driver/{driverId}",produces = "text/event-stream;charset=utf-8")
    public String getData(@PathVariable("driverId") int driverId){

        System.out.println("Method entered "+Math.random());
        PreGrabResponse preGrabResponse = listenService.listen(driverId);

        return "data:"+ JsonUtil.toJson(preGrabResponse)+"\n\n";
    }
}
