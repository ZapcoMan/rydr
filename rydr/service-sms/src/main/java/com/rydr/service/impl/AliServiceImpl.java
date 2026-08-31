package com.rydr.service.impl;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.rydr.constant.SmsStatusEnum;
import com.rydr.dao.SmsDao;
import com.rydr.dao.SmsTemplateDao;
import com.rydr.dao.entity.ServiceSmsRecord;
import com.rydr.dao.entity.ServiceSmsTemplate;
import com.rydr.common.dto.sms.SmsSendRequest;
import com.rydr.common.dto.sms.SmsTemplateDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import com.rydr.common.util.JsonUtil;
import com.rydr.service.AliService;
import com.rydr.service.SmsSender;

import lombok.extern.slf4j.Slf4j;
/**
 * @author oi
 */
@Service
@Slf4j
public class AliServiceImpl implements AliService {

	/**
	*   Cache templates used for content replacement.
	*   Entries expire so a template edited in the DB is picked up without a restart.
	 */
    private final ConcurrentMap<String, TemplateCacheEntry> templateMaps = new ConcurrentHashMap<>();

    @Autowired
    private SmsTemplateDao smsTemplateDto;

    @Autowired
    private SmsDao smsDao;

    @Autowired
    private SmsSender smsSender;

    /** How long a template stays cached, in milliseconds. */
    @Value("${sms.template-cache-ttl:300000}")
    private long templateCacheTtlMs;

    /**
     * Send every template in the request to every receiver.
     *
     * @return the number of messages that could not be delivered (0 means all succeeded)
     */
    @Override
    public int sendSms(SmsSendRequest request) {
        log.info(request.toString());

        if (request == null || request.getReceivers() == null || request.getData() == null) {
            log.warn("Empty SMS request, nothing to send");
            return 0;
        }

        int failures = 0;

        for (String phoneNumber : request.getReceivers()) {
            if (StringUtils.isBlank(phoneNumber)) {
                continue;
            }

            for (SmsTemplateDto template : request.getData()) {
                String content = renderContent(template);
                if (content == null) {
                    // Template not configured: count it, but keep processing the remaining ones
                    failures++;
                    continue;
                }

                // One persisted record per (phone number, template)
                ServiceSmsRecord sms = new ServiceSmsRecord();
                sms.setPhoneNumber(phoneNumber);
                sms.setSmsContent(content);
                sms.setOperatorName("");

                // When one phone number or template fails, the others must still be sent
                try {
                    String param = template.getTemplateMap() == null
                            ? "{}" : JsonUtil.toJson(template.getTemplateMap());
                    int result = smsSender.send(phoneNumber, template.getId(), param);

                    if (result != SmsStatusEnum.SEND_SUCCESS.getCode()) {
                        throw new IllegalStateException("SMS provider did not accept the message");
                    }
                    sms.setSendFlag(1);
                    sms.setSendNumber(0);
                } catch (Exception e) {
                    failures++;
                    sms.setSendFlag(0);
                    sms.setSendNumber(1);
                    log.error("Failed to send SMS (" + template.getId() + "): " + phoneNumber, e);
                } finally {
                    sms.setSendTime(new Date());
                    sms.setCreateTime(new Date());
                    smsDao.insert(sms);
                }
            }
        }
        return failures;
    }

    /**
     * Load the template body and replace its {@code ${placeholder}} tokens.
     *
     * @return the rendered content, or {@code null} when the template is not configured
     */
    private String renderContent(SmsTemplateDto template) {
        if (template == null || StringUtils.isBlank(template.getId())) {
            log.error("SMS request contains a template without an id");
            return null;
        }

        String content = loadTemplateContent(template.getId());
        if (content == null) {
            return null;
        }

        if (template.getTemplateMap() != null) {
            for (Map.Entry<String, Object> entry : template.getTemplateMap().entrySet()) {
                content = StringUtils.replace(content, "${" + entry.getKey() + "}", "" + entry.getValue());
            }
        }
        return content;
    }

    /**
     * Read the template body through the cache.
     *
     * {@link ConcurrentHashMap#compute} guarantees a single DB query per key even when several
     * threads miss at the same time, and returning {@code null} drops the entry so an unknown
     * template is never cached.
     */
    private String loadTemplateContent(String templateId) {
        long now = System.currentTimeMillis();
        TemplateCacheEntry entry = templateMaps.compute(templateId, (id, existing) -> {
            if (existing != null && !existing.isExpired(now)) {
                return existing;
            }
            ServiceSmsTemplate t = smsTemplateDto.getByTemplateId(id);
            if (t == null || StringUtils.isBlank(t.getTemplateContent())) {
                return null;
            }
            return new TemplateCacheEntry(t.getTemplateContent(), now + templateCacheTtlMs);
        });

        if (entry == null) {
            log.error("SMS template [{}] is not configured, the message will not be sent", templateId);
            return null;
        }
        return entry.getContent();
    }

    /**
     * Cached template body with a simple time based expiry.
     */
    private static final class TemplateCacheEntry {

        private final String content;
        private final long expireAt;

        private TemplateCacheEntry(String content, long expireAt) {
            this.content = content;
            this.expireAt = expireAt;
        }

        private String getContent() {
            return content;
        }

        private boolean isExpired(long now) {
            return now >= expireAt;
        }
    }
}
