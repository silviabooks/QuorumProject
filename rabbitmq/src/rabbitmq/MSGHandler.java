package rabbitmq;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Il MSGHandler salva il log sul DB solo se l’oggetto è 
 * “Error receiving object from	“ e l’IP è 10.18.122.24 o 10.18.122.30
 * Sul DB viene salvato: Timestamp – IdMacchina (qual è l'ID macchina?!?) - Messaggio	
 * @author silvia
 */
public class MSGHandler {
    // TODO: vedere come alzare l'interfaccia REST
    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        /* 
         * Patterns to match IP, ID and timestamp
         * note: machineIdPattern matches id plus 'WARN  ' and ' - '
         * this is done to simplify the matching 
        */
        Pattern timestampPattern = 
                Pattern.compile("\\d{4}(-\\d{2}){2} (\\d{2}:){2}\\d{2}.\\d{3}");
        Pattern machineIDPattern = Pattern.compile("WARN( )+.+ - ");
        // We assume that the body of the message is all that follows " - "
        Pattern msgPattern = Pattern.compile(" - .*");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                    AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                // System.out.println(" [x] Received '" + message + "'");
                // analyze string and check for particular message and IP address
                if (message.contains("Error receiving object from") &
                        (message.contains("10.18.122.24") | 
                        message.contains("10.18.122.30"))) {
                    // extract timestamp, machine ID and body from message
                    Matcher machineIDMatcher = machineIDPattern.matcher(message);
                    if (machineIDMatcher.find()) {
                        String machineID = machineIDMatcher.group();
                        // strip from useless characters
                        machineID = machineID.replaceAll("WARN  ", "").replaceAll(" - ", "");
                        System.out.println(machineID); // to be saved in db
                    }
                    Matcher timestampMatcher = timestampPattern.matcher(message);
                    if (timestampMatcher.find()) {
                        String timestamp = timestampMatcher.group();
                        System.out.println(timestamp); // to be saved in db
                    }
                    Matcher msgMatcher = msgPattern.matcher(message);
                    if (msgMatcher.find()) {
                        String msg = msgMatcher.group();
                        System.out.println(msg); // to be saved in db
                    }
                }
            }
        };
        channel.basicConsume(queueName, true, consumer);
    }
}
