package msghandler.thread;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Util.Log;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Il MSGHandler salva il log sul DB solo se l’oggetto è 
 * “Error receiving object from	“ e l’IP è 10.18.122.24 o 10.18.122.30
 * Sul DB viene salvato: Timestamp – IdMacchina - Messaggio	
 * @author silvia
 */
public class MSGHandlerThread implements Runnable {
  // Private variables
  private static final String EXCHANGE_NAME = "logs";
  private static final String URL_POST = 
          "http://localhost:8080/QuorumProject-war/gestione/log/post";
  private final String ipAddress;

  public MSGHandlerThread(String ipAddress) {
      this.ipAddress = ipAddress;
  }

  @Override
  public void run() {
    try {
      // RabbitMQ factory
      ConnectionFactory factory = new ConnectionFactory();
      // Connect to local RabbitMQ queue
      factory.setHost("localhost");
      Connection connection = factory.newConnection();
      // Channel creation and queue binding
      Channel channel = connection.createChannel();
      channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
      String queueName = channel.queueDeclare().getQueue();
      channel.queueBind(queueName, EXCHANGE_NAME, "");

      /* 
       * RegEx Patterns to match IP, ID and timestamp
       * note: machineIdPattern matches id plus 'WARN  ' and ' - '
       * this is done to simplify the matching 
      */
      Pattern timestampPattern = 
              Pattern.compile("\\d{4}(-\\d{2}){2} (\\d{2}:){2}\\d{2}.\\d{3}");
      Pattern machineIDPattern = Pattern.compile("WARN( )+.+ - ");
      // We assume that the body of the message is all that follows " - "
      Pattern msgPattern = Pattern.compile(" - .*");

      // Instantiation of the object needed in handleDelivery
      Log log = new Log();
      Client client = Client.create();
      Gson reqGson = new Gson();
      SimpleDateFormat dateFormat = 
              new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
      Timestamp timestampSql = new java.sql.Timestamp(0);

      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

      Consumer consumer = new DefaultConsumer(channel) {
        /**
         *  Function called for each element in the queue
         * @param consumerTag
         * @param envelope
         * @param properties
         * @param body
         * @throws IOException 
         */
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                AMQP.BasicProperties properties, byte[] body) 
                throws IOException {
          String message = new String(body, "UTF-8");
          // analyze string and check for particular message and IP address
          String timestamp = null, machineID = null, msg = null;
          if (message.contains("Error receiving object from") &
                  (message.contains(ipAddress))) {
            // extract timestamp, machine ID and body from message
            Matcher machineIDMatcher = machineIDPattern.matcher(message);
            if (machineIDMatcher.find()) {
              machineID = machineIDMatcher.group();
              // strip from useless characters
              machineID = machineID.replaceAll("WARN  ", "")
                      .replaceAll(" - ", "");
            }
            Matcher timestampMatcher = timestampPattern.matcher(message);
            if (timestampMatcher.find()) {
              timestamp = timestampMatcher.group();
              // convert timestamp string to java.sql.Timestamp
              try {
                Date parsedDate = dateFormat.parse(timestamp);
                timestampSql.setTime(parsedDate.getTime());
              } catch (ParseException ex) {
                Logger.getLogger(MSGHandlerThread.class.getName())
                        .log(Level.SEVERE, null, ex);
              }
            }
            Matcher msgMatcher = msgPattern.matcher(message);
            if (msgMatcher.find()) {
              msg = msgMatcher.group();
              msg = msg.replaceAll(" - ", "");
            }
          }
          // If all variables are set, the entry has to be saved on DB
          if(timestamp != null && machineID != null && msg != null) {
            log.setIdMacchina(machineID);
            log.setMessage(msg);
            log.setTimestamp(timestampSql);
            System.out.println("SENDING: " + log.toString());
            String reqString = reqGson.toJson(log);
            WebResource webResourcePost = client.resource(URL_POST);
            ClientResponse rispostaPost = webResourcePost
                    .post(ClientResponse.class, reqString);
            System.out.println("RECEIVED: "
                    + rispostaPost.getEntity(String.class));
          }
        }
      };
      channel.basicConsume(queueName, true, consumer);
    } catch (Exception ex) {
      Logger.getLogger(MSGHandlerThread.class.getName())
                .log(Level.SEVERE, null, ex);
    }
  }
}
