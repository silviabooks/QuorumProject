/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msghandler.worker;

import Util.Log;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Il MSGHandler salva il log sul DB solo se l’oggetto è 
 * “Error receiving object from	“ e l’IP è 10.18.122.24 o 10.18.122.30
 * Sul DB viene salvato: Timestamp – IdMacchina - Messaggio	
 * @author silvia
 */
public class WorkerMSGHandler {
    // *** Variables declaration ***
    private static final String TASK_QUEUE_NAME = "task_queue";
    private static final String EXCHANGE_NAME = "logs";
    private static final String URL_POST = 
            "http://localhost:8080/QuorumProject-war/gestione/log/post";
    // RegEx Patterns
    private static final Pattern timestampPattern
            = Pattern.compile("\\d{4}(-\\d{2}){2} (\\d{2}:){2}\\d{2}.\\d{3}");
    private static final Pattern machineIDPattern = 
            Pattern.compile("WARN( )+.+ - ");
    // We assume that the body of the message is all that follows " - "
    private static final Pattern msgPattern = Pattern.compile(" - .*");
    
    // Instantiation of the object needed in handleDelivery
    private static Log log = new Log();
    private static Client client = Client.create();
    private static Gson reqGson = new Gson();
    private static SimpleDateFormat dateFormat = 
            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    private static Timestamp timestampSql = new java.sql.Timestamp(0);
    
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();
                
        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, 
                    AMQP.BasicProperties properties, byte[] body) 
                    throws IOException {
                String message = new String(body, "UTF-8");
                
                System.out.println(" [x] Received '" + message + "'");
                try {
                    doWork(message);
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
    channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
  }

    private static void doWork(String message) {
        
        // analyze string and check for particular message and IP address
        String timestamp = null, machineID = null, msg = null;
        if (message.contains("Error receiving object from") &
                (message.contains("10.18.122.24") | 
                message.contains("10.18.122.30"))) {
            // extract timestamp, machine ID and body from message
            Matcher machineIDMatcher = machineIDPattern.matcher(message);
            if (machineIDMatcher.find()) {
                machineID = machineIDMatcher.group();
                // strip from useless characters
                machineID = machineID.replaceAll("WARN  ", "")
                        .replaceAll(" - ", "");
                System.out.println(machineID);
            }
            Matcher timestampMatcher = timestampPattern.matcher(message);
            if (timestampMatcher.find()) {
                timestamp = timestampMatcher.group();
                System.out.println(timestamp);
                // convert timestamp string to java.sql.Timestamp
                try {
                    Date parsedDate = dateFormat.parse(timestamp);
                    timestampSql.setTime(parsedDate.getTime());
                } catch (ParseException ex) {
                    Logger.getLogger(WorkerMSGHandler.class.getName())
                            .log(Level.SEVERE, null, ex);
                }

            }
            Matcher msgMatcher = msgPattern.matcher(message);
            if (msgMatcher.find()) {
                msg = msgMatcher.group();
                msg = msg.replaceAll(" - ", "");
                System.out.println(msg);
            }
        }
        if(timestamp != null && machineID != null && msg != null) {
            log.setIdMacchina(machineID);
            log.setMessage(msg);
            log.setTimestamp(timestampSql);
            System.out.println("VOGLIO INVIARE: " + log.toString());
            String reqString = reqGson.toJson(log);
            WebResource webResourcePost = client.resource(URL_POST);
            ClientResponse rispostaPost = webResourcePost
                    .post(ClientResponse.class, reqString);
            System.out.println("HO RICEVUTO: "
                    + rispostaPost.getEntity(String.class));
            // Sleep to avoid my PC to explode
            /*try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(WorkerMSGHandler.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
            */
        }
    }
}
