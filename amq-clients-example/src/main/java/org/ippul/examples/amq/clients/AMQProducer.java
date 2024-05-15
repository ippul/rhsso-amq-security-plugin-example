package org.ippul.examples.amq.clients;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import javax.jms.*;
import java.util.UUID;

public class AMQProducer {

    private final String brokerUrl;
    
    private final String queueName;
    
    private final String username;
    
    private final String password;

    public AMQProducer(final String brokerUrl, final String queueName, final String username, final String password){
        this.brokerUrl = brokerUrl;
        this.queueName = queueName;
        this.username = username;
        this.password = password;
    }

    public void produce() throws Exception {
        try(final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl, username, password);
            final Connection connection = connectionFactory.createConnection();) {      
            connection.start();
            try(final Session session  = connection.createSession()) {
                final Destination destination = session.createQueue(queueName);
                try(final MessageProducer producer = session.createProducer(destination)) {
                    for(int count = 0; count < 1_000_000; count ++) {
                        final TextMessage message = session.createTextMessage("Test JMS Message " + UUID.randomUUID().toString());
                        System.out.println("Message body: " + message.getBody(String.class));
                        producer.send(message);
                        Thread.sleep(10l);
                    }
                }
            }
        }
    }
}
