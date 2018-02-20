package scanner.thread;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
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
public class Scanner {
  private static final String EXCHANGE_NAME = "logs";

  public static void main(String[] argv) throws Exception {
    // RabbitMQ factory
    ConnectionFactory factory = new ConnectionFactory();
    // Connect to local RabbitMQ queue
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);

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
          channel.basicPublish(EXCHANGE_NAME, "", null, 
                  logLine.getBytes("UTF-8"));
          System.out.println(" [x] Sent '" + logLine + "'");
        }
      }
      System.out.println("Scanning complete!");
      logReader.close();
      channel.close();
      connection.close();
    } catch (FileNotFoundException e) {
      Logger.getLogger(Scanner.class.getName()).log(Level.SEVERE, null, e);
      System.exit(0);
    }
  }
}
