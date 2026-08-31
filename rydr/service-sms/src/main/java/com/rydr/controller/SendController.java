package com.rydr.controller;

import com.rydr.common.dto.sms.SmsSendRequest;
import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.service.AliService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.rydr.common.util.JsonUtil;
import com.rydr.dto.ResponseResult;

import lombok.extern.slf4j.Slf4j;
/**
 * @author oi
 */
@RestController
@RequestMapping("/send")
@Slf4j
public class SendController {

	@Autowired
	private AliService aliService;

	@RequestMapping(value = "/alisms-template",method = RequestMethod.POST)
    public ResponseResult send(@RequestBody SmsSendRequest smsSendRequest){
		// Output received parameter content
        String param = JsonUtil.toJson(smsSendRequest);
        log.info("/send/alisms-template   request: "+param);

        int failures = aliService.sendSms(smsSendRequest);
        if (failures > 0) {
            // Report the real outcome instead of always claiming success
            return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(),
                    failures + " message(s) could not be sent");
        }
        return  ResponseResult.success("");
    }

}
