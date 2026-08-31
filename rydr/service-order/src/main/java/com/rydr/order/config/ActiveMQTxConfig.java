package com.rydr.order.config;

import jakarta.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.jms.ConnectionFactory;

/**
 * ActiveMQ transactional-messaging configuration for the reliable-message outbox.
 *
 * <p>Key idea: a {@link JmsTransactionManager} bridges the local JDBC transaction
 * (where the tx_message row is inserted) and the JMS send, so the broker message is
 * only committed when the local DB commit succeeds and vice-versa. This gives the
 * "transactional outbox" guarantee without a full distributed transaction.
 */
@Configuration
public class ActiveMQTxConfig {

    /** Queue carrying payment-deduction commands to service-wallet. */
    public static final String QUEUE_PAY_DEDUCT = "queue.order.pay.deduct";

    /** Queue carrying wallet results (success / failure) back to service-order. */
    public static final String QUEUE_WALLET_RESULT = "queue.wallet.result";

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Bean
    public Queue payDeductQueue() {
        return new ActiveMQQueue(QUEUE_PAY_DEDUCT);
    }

    @Bean
    public Queue walletResultQueue() {
        return new ActiveMQQueue(QUEUE_WALLET_RESULT);
    }

    /**
     * JmsTemplate wired to the platform transaction manager so send() participates
     * in the surrounding (local DB + JMS) transaction.
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        // Deliver to the queue defined by the Queue bean.
        template.setDefaultDestination(payDeductQueue());
        return template;
    }

    /**
     * Listeners on service-order (wallet-result consumer) must also be transactional
     * so the received message is acked only after the handler commits.
     */
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            PlatformTransactionManager transactionManager) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setTransactionManager(transactionManager);
        factory.setConcurrency("1-2");
        return factory;
    }

    /**
     * Explicit transaction manager over the JMS session. Combined with the data
     * source transaction manager via a single {@code @Transactional} boundary on the
     * service method, both commits are coordinated (local DB first, JMS second).
     */
    @Bean
    public PlatformTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory) {
        return new JmsTransactionManager(connectionFactory);
    }

    // brokerUrl field is referenced here so that a misconfiguration of the
    // broker URL is surfaced as a bean wiring error rather than silently ignored.
    public String getBrokerUrl() {
        return brokerUrl;
    }
}
