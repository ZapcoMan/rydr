package com.rydr.service;

/**
 * Transport that actually hands a rendered SMS over to a provider.
 *
 * The bundled implementation is a {@code console} transport meant for local development.
 * A real gateway (Aliyun, Tencent, ...) can be added as another implementation and selected
 * with the {@code sms.provider} property, without touching the sending logic.
 *
 * @author oi
 */
public interface SmsSender {

    /**
     * @param phoneNumber recipient phone number
     * @param templateCode provider template id
     * @param param rendered template parameters
     * @return {@link com.rydr.constant.SmsStatusEnum#SEND_SUCCESS} when the provider accepted
     *         the message, {@link com.rydr.constant.SmsStatusEnum#SEND_FAIL} otherwise
     */
    int send(String phoneNumber, String templateCode, String param);
}
