package com.rydr.rabbitmq;

import jakarta.annotation.Resource;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Functional Spring Cloud Stream producer (replaces the removed @EnableBinding(Source.class)).
 * StreamBridge sends to the "output-out-0" binding configured in bootstrap.yml
 */
@RestController
@RequestMapping("/rabbitmq")
public class MyStreamSend {

    @Resource
    private StreamBridge streamBridge;

    @PostMapping("/send")
    public String sendTestData(@RequestBody String content) {
        // Send message
        streamBridge.send("output-out-0", content);
        return "Send successful";
    }
}
