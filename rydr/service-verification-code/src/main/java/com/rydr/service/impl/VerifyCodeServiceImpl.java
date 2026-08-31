package com.rydr.service.impl;

import com.rydr.common.constant.CommonStatusEnum;
import com.rydr.dto.ResponseResult;
import com.rydr.common.dto.verificationcode.VerifyCodeResponse;
import com.rydr.common.util.RedisKeyUtil;
import com.rydr.service.VerifyCodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;
/**
 * @author oi
 */
@Service
public class VerifyCodeServiceImpl implements VerifyCodeService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Generate verification code
     * @param identity
     * @param phoneNumber
     * @return
     */
    @Override
    public ResponseResult generate(int identity , String phoneNumber){

        // Validate send time limit, three-tier verification, cannot send SMS without restrictions
        ResponseResult limit = checkSendCodeTimeLimit(phoneNumber);
        if (limit != null && limit.getCode() != com.rydr.constatnt.BusinessInterfaceStatus.SUCCESS.getCode()) {
            return limit;
        }

        String code = String.valueOf((int)((Math.random()*9+1)*Math.pow(10,5)));

        // Generate Redis key
        String keyPre = RedisKeyUtil.generateKeyPreByIdentity(identity);
        String key = keyPre + phoneNumber;
        // Store in Redis, expires in 2 minutes
        BoundValueOperations<String, String> codeRedis = redisTemplate.boundValueOps(key);
        // Expires in 2 minutes
        codeRedis.set(code,120, TimeUnit.SECONDS);

        // Return result
        VerifyCodeResponse result = new VerifyCodeResponse();
        result.setCode(code);
        return ResponseResult.success(result);
    }

    @Override
    public ResponseResult verify(int identity,String phoneNumber,String code){
        // Three-tier verification


        // Generate Redis key
        String keyPre = RedisKeyUtil.generateKeyPreByIdentity(identity);
        String key = keyPre + phoneNumber;
        BoundValueOperations<String, String> codeRedis = redisTemplate.boundValueOps(key);
        String redisCode = codeRedis.get();

        if(StringUtils.isNotBlank(code)
                && StringUtils.isNotBlank(redisCode)
                && code.trim().equals(redisCode.trim())) {
            return ResponseResult.success("");
        }else {
            return ResponseResult.fail(CommonStatusEnum.VERIFY_CODE_ERROR.getCode(), CommonStatusEnum.VERIFY_CODE_ERROR.getValue());
        }

    }


    /**
     * Check send time limit for this phone number.
     *
     * Three-tier rate limiting using Redis counters with sliding TTL:
     *  - within 1 minute:   at most 1 send
     *  - within 10 minutes: at most 3 sends
     *  - within 24 hours:   at most 5 sends
     *
     * @param phoneNumber phone number
     * @return {@link ResponseResult#success} when allowed, a failure with the
     *         corresponding {@link CommonStatusEnum} code otherwise
     */
    private ResponseResult checkSendCodeTimeLimit(String phoneNumber){
        if (StringUtils.isBlank(phoneNumber)) {
            return ResponseResult.success("");
        }

        long[] windows = {60, 600, 86400};
        int[] limits = {1, 3, 5};
        CommonStatusEnum[] hitEnum = {
                CommonStatusEnum.VERIFICATION_ONE_MIN_ERROR,
                CommonStatusEnum.VERIFICATION_TEN_MIN_ERROR,
                CommonStatusEnum.VERIFICATION_ONE_HOUR_ERROR
        };

        for (int i = 0; i < windows.length; i++) {
            String limitKey = "verify_code_limit_" + windows[i] + "_" + phoneNumber;
            BoundValueOperations<String, String> ops = redisTemplate.boundValueOps(limitKey);
            Long count = ops.increment();
            if (count != null && count == 1) {
                // First hit in this window: set the expiry so the counter resets automatically
                ops.expire(windows[i], TimeUnit.SECONDS);
            }
            if (count != null && count > limits[i]) {
                return ResponseResult.fail(hitEnum[i].getCode(), hitEnum[i].getValue());
            }
        }

        return ResponseResult.success("");
    }
}
