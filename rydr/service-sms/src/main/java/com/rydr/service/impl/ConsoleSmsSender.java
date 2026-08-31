package com.rydr.service.impl;

import com.rydr.constant.SmsStatusEnum;
import com.rydr.service.SmsSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Local development transport: the rendered message is "delivered" to the application log,
 * so that the registration / login flow can be exercised without real SMS credentials.
 *
 * Set {@code sms.provider} to any other value to make sending fail loudly instead of
 * silently pretending the message went out.
 *
 * @author oi
 */
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "console", matchIfMissing = true)
@Slf4j
public class ConsoleSmsSender implements SmsSender {

    /** The only transport bundled with this project. */
    private static final String PROVIDER_CONSOLE = "console";

    @Value("${sms.provider:console}")
    private String provider;

    @Override
    public int send(String phoneNumber, String templateCode, String param) {
        if (!PROVIDER_CONSOLE.equalsIgnoreCase(provider)) {
            log.error("SMS provider [{}] has no implementation in this project; "
                    + "set sms.provider=console for local runs", provider);
            return SmsStatusEnum.SEND_FAIL.getCode();
        }

        if (StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(templateCode)) {
            log.error("Refusing to send SMS with blank phone number or template code");
            return SmsStatusEnum.SEND_FAIL.getCode();
        }

        log.info("[sms:console] to={}, template={}, param={}", phoneNumber, templateCode, param);
        return SmsStatusEnum.SEND_SUCCESS.getCode();
    }
}
