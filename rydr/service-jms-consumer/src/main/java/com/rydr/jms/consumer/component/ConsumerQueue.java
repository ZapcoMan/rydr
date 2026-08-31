package com.rydr.jms.consumer.component;

import com.rydr.jms.consumer.config.ActiveMQConfig;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * Message consumer (Queue mode)
 */
@Component
public class ConsumerQueue {
    /**
     * Use JmsListener to configure the queue monitored by the consumer, where text is the received message
     * @param text
     */
   @JmsListener(destination = ActiveMQConfig.QUEUE_NAME,containerFactory = "jmsListenerContainerQueue")
   public void receiveQueue1(TextMessage text, Session session) throws JMSException {
       try {
//           int i = 1/0;
           System.out.println("Consumer received queue1 message: "+text.getText());

           // Acknowledge after committing the transaction. Because even if an event comes again next time, the database insert will fail.
           text.acknowledge();

       }catch (Exception e){
            session.recover();
           try {
               Thread.sleep(5000);
           } catch (InterruptedException e1) {
               e1.printStackTrace();
           }
           System.out.println("Exception occurred");
       }finally {

       }

   }

//    @JmsListener(destination = ActiveMQConfig.QUEUE_NAME)
//    public void receiveQueue2(String text) {
//        System.out.println("Consumer received queue2 message: "+text);
//    }
}
