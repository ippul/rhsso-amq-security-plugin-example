package org.ippul.examples;

import java.nio.file.Files;
import java.nio.file.Path;

import org.ippul.examples.amq.clients.*;

public class Application {
    
    private static final String BROKER_URL = "tcp://ex-aao-hdls-svc:61616";

    public static void main(String[] args) throws Exception {
        final String worktype = System.getenv("WORK_TYPE");
        final String username = Files.readString(Path.of("/etc/secrets/username"));
        final String password = Files.readString(Path.of("/etc/secrets/password"));
        switch (worktype) {
            case "PRODUCER":
                new AMQProducer(BROKER_URL, "queue1", username, password).produce();
                System.exit(0);
                break;
            case "CONSUMER":
                new AMQConsumer(BROKER_URL, "queue1", username, password).consume();
                System.exit(0);
                break;
            default:
                System.err.println("Env variable 'WORK_TYPE' not recognised found '" + worktype + "' expected 'PRODUCER' or 'CONSUMER'");
                System.exit(1);
                break;
        }
    }
}
