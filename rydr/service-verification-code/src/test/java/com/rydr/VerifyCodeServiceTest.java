package com.rydr;

import com.rydr.dto.ResponseResult;
import com.rydr.common.dto.verificationcode.VerifyCodeResponse;
import com.rydr.controller.VerifyCodeController;
import com.rydr.service.VerifyCodeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
//@Transactional
@Slf4j
public class VerifyCodeServiceTest {

    @Autowired
    VerifyCodeService verifyCodeService;

    String phoneNumber = "13800000001";


    @Test
    public void generate(){
        String msgCode = "";
        ResponseResult generateResult = verifyCodeService.generate(1,phoneNumber);
        int code = generateResult.getCode();
        Assertions.assertEquals(0,code);

        if (code == 0){
            VerifyCodeResponse data = (VerifyCodeResponse)generateResult.getData();
            msgCode = data.getCode();
        }

        Assertions.assertEquals(6,msgCode.length());
    }

    @Test
    public void verify(){
        ResponseResult generateResult = verifyCodeService.generate(1,phoneNumber);
        VerifyCodeResponse data = (VerifyCodeResponse)generateResult.getData();
        String msgCode = data.getCode();
        log.info("msgCode:"+msgCode);
        ResponseResult result = verifyCodeService.verify(1,phoneNumber,msgCode);
        int code = result.getCode();

        Assertions.assertEquals(0,code);
    }
}
