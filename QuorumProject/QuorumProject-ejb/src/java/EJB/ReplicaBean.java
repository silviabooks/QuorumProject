/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Connettore.ConnettoreMySQL;
import Util.ElementQueue;
import Util.ElementQueueComparator;
import Util.Log;
import Util.VersionNumber;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;

/**
 *
 * @author zartyuk
 */
@Stateless(name = "firstReplica")
public class ReplicaBean implements ReplicaBeanLocal {

    @EJB
    private FaultDetectorLocal faultDetector;
    
    private VersionNumber num = new VersionNumber(0,1);
    
    private LinkedList<ElementQueue> queue = new LinkedList<>();

    @Override
    public VersionNumber getNum() {
        return num;
    }
    
    @Override
    public String readReplica() throws SQLException {
        ArrayList<Log> logs = new ArrayList<>();
        String query = "SELECT * FROM LOG";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        ResultSet rs = connettore.doQuery(query);
        
        while(rs.next()){
            logs.add(new Log(rs.getTimestamp("timestamp"), rs.getString("idMacchina"),rs.getString("message")));
        }
        connettore.close();
        return new Gson().toJson(logs);
    }
    
    @Override
    public void writeReplica(Log l) {
        System.out.println(l.getTimestamp().toString());
        queue.add(new ElementQueue(num, l, false));
        Collections.sort(queue, new ElementQueueComparator());
        System.out.println(queue.toString());
        try {
            serialize("replica1queue.dat");
        } catch (IOException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateDatabase(Log l) throws SQLException {
        String update = "INSERT INTO LOG VALUES(" + "\'" + l.getTimestamp() + "\'," + "\'" + l.getIdMacchina() + "\', "+ "\'" + l.getMessage() + "\'" +")";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        connettore.doUpdate(update);
        connettore.close();
    }
    
    @Override
    public void test() {
        System.out.println("Sono la prima replica");
    }
    
    @Override
    public void updateVersionNumber(int timestamp, Log l) {
        num.setTimestamp(timestamp);
        for(ElementQueue e : queue) {
            if(e.getLog().equals(l)) {
                e.setConfirmed(true);
                e.getNum().setTimestamp(timestamp);
                System.out.println("sort");
            }
        }
        Collections.sort(queue, new ElementQueueComparator());
        if(queue.peek().isConfirmed() == true) {
            for (ElementQueue e : queue) {
                if(e.isConfirmed() == true) try {
                    updateDatabase(e.getLog());
                    queue.remove(e);
                } catch (SQLException ex) {
                    Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void serialize(String string) throws IOException {
        //FileOutputStream fout = null;
        ObjectOutputStream oos = null;
        System.out.println("Per adesso ok");
        try {
            //fout = new FileOutputStream(string);
            //System.out.println("ok ok");
            oos = new ObjectOutputStream(new FileOutputStream(string));
            System.out.println("problem?");
            for (ElementQueue e : queue) {
                oos.writeObject(e);
                System.out.println("wow");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            oos.close();
            //fout.close();
        }
    }
    
    @Override
    public void unserialize(String string) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(string));
            while (ois.available() > 0) {
                queue.add((ElementQueue) ois.readObject());
                System.out.println("Mbare ho trovato un elemento");
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File not found! Your queue will be empty!");
           // queue = new LinkedList<>();
        } catch (IOException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if(ois!= null) ois.close();
                //fin.close();
            } catch (IOException ex) {
                Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Schedule(second="2", persistent = false)
    public void atSchedule() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        if(connettore.testConnection(2)) {
            faultDetector.receive(0);
        }
        try {
            connettore.close();
        } catch (SQLException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public boolean pingAckResponse() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        if(connettore.testConnection(5)) {
            try {
                connettore.close();
            } catch (SQLException ex) {
                //Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("l");
            }
            return true;
        }
        try {
            connettore.close();
        } catch (SQLException ex) {
            //Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("55");
        }
        return false;
    }
}
