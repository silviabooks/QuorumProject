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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;

/**
 *
 * @author zartyuk
 */
@Stateless(name = "secondReplica")
public class ReplicaBean2 implements ReplicaBeanLocal {

    @EJB
    private FaultDetectorLocal faultDetector;
    
    private static VersionNumber num;
    
    private static List<ElementQueue> queue = 
            Collections.synchronizedList(new LinkedList<>());
    
    @Override
    public void init() {
        this.num = new VersionNumber(0,2);
        System.out.println(this.toString() + " trying to find an existing queue");
        unserialize("replica2queue.dat");
    }
    
    @Override
    public VersionNumber getNum() {
        //ConnettoreMySQL connettore = new ConnettoreMySQL("3307");
        //if(connettore.testConnection(2)) {
        //    connettore.close();
            return this.num;
        //}
        //else {
        //    connettore.close();
        //    return null;
        //}
    }
    
    @Override
    public String readReplica() throws SQLException {
        ArrayList<Log> logs = new ArrayList<>();
        String query = "SELECT * FROM LOG";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3307");
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
        queue.add(new ElementQueue(this.num, l, false));
        this.num.setTimestamp(this.num.getTimestamp()+1);
        Collections.sort(queue, new ElementQueueComparator());
        Iterator<ElementQueue> iter = queue.iterator();
        while (iter.hasNext()) {
            ElementQueue e = iter.next();
            System.out.print("Element in replica2queue: " + 
                    e.getLog().toString() + " " + 
                    e.isConfirmed() + " " + 
                    e.getNum().getTimestamp());
        }
    }
    
    private void updateDatabase(Log l) {
        String update = "INSERT INTO LOG VALUES(" + "\'" + l.getTimestamp() + "\'," 
                + "\'" + l.getIdMacchina() + "\', "+ "\'" + l.getMessage() + "\'" +")";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3307");
        connettore.doUpdate(update);
        connettore.close();
    }
    
    @Override
    public void restoreConsistency(Log l) {
        String delete = "DELETE FROM LOG WHERE timestamp = " + "\'" + 
                l.getTimestamp() + "\' AND idMacchina = " + "\'" + 
                l.getIdMacchina() + "\' AND message = "+ "\'" + 
                l.getMessage() + "\'";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3307");
        connettore.doUpdate(delete);
        connettore.close();
    }
    
    @Override
    public void updateVersionNumber(VersionNumber vn, Log l) {
        Iterator<ElementQueue> iter = queue.iterator();
        while (iter.hasNext()) {
            ElementQueue e = iter.next();
            if(e.getLog().equals(l)) {
                e.setConfirmed(true);
                e.setNum(vn);
                break;
            }
        }
        Collections.sort(queue, new ElementQueueComparator());
        serialize("replica2queue.dat");
        if(this.num.getTimestamp() < vn.getTimestamp()) this.num.setTimestamp(vn.getTimestamp());
    }
    
    @Override
    public boolean commit() {
        if(!queue.isEmpty() && queue.get(0).isConfirmed() == true) {
            Iterator<ElementQueue> ite = queue.iterator();
            while (ite.hasNext()) {
                ElementQueue e = ite.next();
                if(e.isConfirmed()) {
                        try {
                        updateDatabase(e.getLog());
                        ite.remove();
                        }
                        catch (RuntimeException ex) {
                            System.out.println("Problem in storing content. "
                                    + this.toString() + " down");
                            return false;
                        }
                }
                else break;
            }
        }
        serialize("replica2queue.dat");
        return true;
    }

    private void serialize(String string) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(string));
            oos.writeInt(queue.size());
            Iterator<ElementQueue> iter = queue.iterator();
            while (iter.hasNext()) {
                oos.writeObject(iter.next());
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
            int y = ois.readInt();
            while(y!=0) {
                queue.add((ElementQueue) ois.readObject());
                System.out.println("Found a element. Added to replica2queue");
                --y;
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File not found! Your replica2queue will be empty!");
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
    
    @Schedule(second="1/1", minute = "*", hour = "*", persistent = false)
    private void sendHeartBeat() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3307");
        if(connettore.testConnection(2)) {
            System.out.println(this.toString() + " sending HeartBeat");
            faultDetector.receive("second");
        }
        connettore.close();
    }
    
    @Override
    public boolean pingAckResponse() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3307");
        if(connettore.testConnection(5)) {
            connettore.close();
            return true;
        }
        return false;
    }
}
