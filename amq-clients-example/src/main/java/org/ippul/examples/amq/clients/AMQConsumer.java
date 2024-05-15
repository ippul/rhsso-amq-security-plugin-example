package org.ippul.examples.amq.clients;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import javax.jms.*;

public class AMQConsumer {


    private final String brokerUrl;
    
    private final String queueName;
    
    private final String username;
    
    private final String password;

    public AMQConsumer(final String brokerUrl, final String queueName, final String username, final String password){
        this.brokerUrl = brokerUrl;
        this.queueName = queueName;
        this.username = username;
        this.password = password;
    }

    public void consume() throws JMSException, InterruptedException {
        try(final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl, username, password);
            final Connection connection = connectionFactory.createConnection();){      
            connection.start();
            try(final Session session  = connection.createSession();
                final MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));){
                for(int count = 0; count < 1_000_000; count ++) {
                    final Message message = consumer.receive(10000);
                    String messageBody = message.getBody(String.class);
                    System.out.println("Message body: " + messageBody);
                    message.acknowledge();
                    Thread.sleep(1000l);
                }
            }
        }
    }
}
