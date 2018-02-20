/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scanner.worker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SCANNER
 * Legge le righe del file di log e le analizza
 * Ogni	volta che c’è una entry WARN,
 * l’intera riga di log viene pubblicata nella coda.
 * 
 * @author silvia
 */
public class WorkerScanner {
    
    private static final String TASK_QUEUE_NAME = "task_queue";
    
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        
        String currDir = System.getProperty("user.dir");
        
        try {
            // open log file
            BufferedReader logReader = new BufferedReader(
                    new FileReader(currDir + "/log/ERROR_WARN.log"));
            System.out.println("Reading log...");
            // read lines
            while(true) {
                String logLine = logReader.readLine();
                // if there are no more entries, break the loop
                if(logLine == null) break;
                // analyze the string to find WARN and publish in the queue
                if(logLine.contains("WARN")) {
                    channel.basicPublish("", TASK_QUEUE_NAME,
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            logLine.getBytes("UTF-8"));
                            System.out.println(" [x] Sent '" + logLine + "'");
                }
            }
            System.out.println("Scanning complete!");
            logReader.close();
            channel.close();
            connection.close();
        } catch (FileNotFoundException e) {
            Logger.getLogger(WorkerScanner.class.getName())
                    .log(Level.SEVERE, null, e);
            System.exit(0);
        }
    }
}


        