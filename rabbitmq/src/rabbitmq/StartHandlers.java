/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabbitmq;

/**
 *
 * @author silvia
 */
public class StartHandlers {
  public static void main(String[] argv) {
    Thread msg1 = new Thread(new MSGHandlerThread("10.18.122.24"));
    msg1.start();
    Thread msg2 = new Thread(new MSGHandlerThread("10.18.122.30"));
    msg2.start();
  }
}
