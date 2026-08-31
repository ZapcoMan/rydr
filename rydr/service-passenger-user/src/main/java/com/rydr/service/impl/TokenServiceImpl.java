package com.rydr.service.impl;

import com.rydr.common.util.JwtUtil;
import com.rydr.service.TokenService;

import java.util.Date;

import org.springframework.stereotype.Service;

/**
 * @author oi
 */
@Service
public class TokenServiceImpl implements TokenService {

    /**
     * Generate token
     * @param subject
     * @return
     */
    @Override
    public String createToken(String subject) {
        return JwtUtil.createToken(subject, new Date());
    }
}
