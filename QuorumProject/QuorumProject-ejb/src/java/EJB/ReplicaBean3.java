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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;

/**
 *
 * @author zartyuk
 */
@Stateless(name = "thirdReplica")
public class ReplicaBean3 implements ReplicaBeanLocal {

    @EJB
    private FaultDetectorLocal faultDetector;
    
    private VersionNumber num = new VersionNumber(0,3);
    
    private List<ElementQueue> queue = Collections.synchronizedList(new LinkedList<>());
    
    @PostConstruct
    private void init() {
        System.out.println(this.toString() + " trying to find an existing queue");
        unserialize("replica3queue.dat");
    }
    
    @Override
    public VersionNumber getNum() {
        return num;
    }
    
    @Override
    public String readReplica() throws SQLException {
        ArrayList<Log> logs = new ArrayList<>();
        String query = "SELECT * FROM LOG";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3308");
        ResultSet rs = connettore.doQuery(query);
        
        while(rs.next()){
            logs.add(new Log(rs.getTimestamp("timestamp"), 
                    rs.getString("idMacchina"),
                    rs.getString("message")));
        }
        connettore.close();
        return new Gson().toJson(logs);
    }
    
    @Override
    public void writeReplica(Log l) {
        queue.add(new ElementQueue(num, l, false));
        Collections.sort(queue, new ElementQueueComparator());
        System.out.println(queue.toString());
        serialize("replica3queue.dat");
    }
    
    private void updateDatabase(Log l) {
        String update = "INSERT INTO LOG VALUES(" + "\'" + l.getTimestamp() + "\'," 
                + "\'" + l.getIdMacchina() + "\', "+ "\'" + l.getMessage() + "\'" +")";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3308");
        connettore.doUpdate(update);
        connettore.close();
    }
    
    @Override
    public void updateVersionNumber(int timestamp, Log l) {
        num.setTimestamp(timestamp);
        for(ElementQueue e : queue) {
            if(e.getLog().equals(l)) {
                e.setConfirmed(true);
                e.getNum().setTimestamp(timestamp);
            }
        }
        Collections.sort(queue, new ElementQueueComparator());
        if(queue.get(0).isConfirmed() == true) {
            for (ElementQueue e : queue) {
                if(e.isConfirmed() == true) {
                    updateDatabase(e.getLog());
                    queue.remove(e);
                }
            }
        }
    }
    
    private void serialize(String string) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(string));
            for (ElementQueue e : queue) {
                oos.writeObject(e);
                System.out.println("Saving a element in replica3queue!");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(oos != null)
                try {
                    oos.close();
            } catch (IOException ex) {
                Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void unserialize(String string) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(string));
            while (ois.available() > 0) {
                queue.add((ElementQueue) ois.readObject());
                System.out.println("Found a element. Added to replica3queue");
            }
            if(ois.available() == 0) 
                System.out.println("File found but is empty. Your replica3queue will be empty!");
        } catch (FileNotFoundException ex) {
            System.out.println("File not found! Your replica3queue will be empty!");
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if(ois != null) 
                    ois.close();
            } catch (IOException ex) {
                Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Schedule(second="5/5", minute = "*", hour = "*", persistent = false)
    private void sendHeartBeat() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3308");
        if(connettore.testConnection(2)) {
            System.out.println(this.toString() + " sending HeartBeat");
            faultDetector.receive("third");
        }
        connettore.close();
    }
    
    @Override
    public boolean pingAckResponse() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3308");
        if(connettore.testConnection(5)) {
            connettore.close();
            return true;
        }
        return false;
    }
}
