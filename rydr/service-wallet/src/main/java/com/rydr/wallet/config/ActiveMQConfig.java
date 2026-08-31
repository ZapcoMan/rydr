package com.rydr.wallet.config;

import jakarta.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;

/**
 * ActiveMQ configuration for service-wallet.
 *
 * <p>Consumes payment-deduction commands from {@code queue.order.pay.deduct} and replies
 * on {@code queue.wallet.result}. The listener container is transactional so a message is
 * acked only after the wallet deduction handler commits.
 */
@Configuration
public class ActiveMQConfig {

    /** Queue carrying payment-deduction commands to service-wallet. */
    public static final String QUEUE_PAY_DEDUCT = "queue.order.pay.deduct";

    /** Queue carrying wallet results (success / failure) back to service-order. */
    public static final String QUEUE_WALLET_RESULT = "queue.wallet.result";

    @Bean
    public Queue payDeductQueue() {
        return new ActiveMQQueue(QUEUE_PAY_DEDUCT);
    }

    @Bean
    public Queue walletResultQueue() {
        return new ActiveMQQueue(QUEUE_WALLET_RESULT);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestination(walletResultQueue());
        return template;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setConcurrency("1-2");
        return factory;
    }
}
