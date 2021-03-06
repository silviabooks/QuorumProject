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
 * ReplicaManager della replica 3310
 * Codice commentato nella docs 3306
 * @author zartyuk
 */

@Stateless(name = "fifthReplica")
public class ReplicaBean5 implements ReplicaBeanLocal {

    @EJB
    private FaultDetectorLocal faultDetector;
    
    private static VersionNumber num;
    
    private static List<ElementQueue> queue = 
            Collections.synchronizedList(new LinkedList<>());
    
    @Override
    public void init() {
        this.num =  new VersionNumber(0,5);
        System.out.println(this.toString() + " trying to find an existing queue");
        unserialize("replica5queue.dat");
    }
    
    @Override
    public VersionNumber getNum() {
        return this.num;
    }
    
    @Override
    public String queryReadReplica(String q) {
        try {
            ArrayList<Log> logs = new ArrayList<>();
            ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
            ResultSet rs = connettore.doQuery(q);
            while(rs.next()) {
                logs.add(new Log(rs.getTimestamp("timestamp"),
                        rs.getString("idMacchina"),
                        rs.getString("message")));
            }
            connettore.close();
            return new Gson().toJson(logs);
        } catch (SQLException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            System.out.println("Replica 5 was selected, but is probably faultly. Retry the read!");
        }
        return "Selected a fault replica. Retry!";
    }
    
    
    @Override
    public void writeReplica(Log l) {
        num.setTimestamp(num.getTimestamp()+1);
        queue.add(new ElementQueue(new VersionNumber(num.getTimestamp(), num.getId()), l, false));
        Collections.sort(queue, new ElementQueueComparator());
        
        /*Iterator<ElementQueue> iter = queue.iterator();
        while (iter.hasNext()) {
            ElementQueue e = iter.next();
            System.out.print("Element in replica5queue: " + 
                    e.getLog().toString() + " " + 
                    e.isConfirmed() + " " + 
                    e.getNum().getTimestamp());
        }*/
    }
    
    private void updateDatabase(Log l) {
        String update = "INSERT INTO LOG VALUES(" + "\'" + l.getTimestamp() + "\'," 
                + "\'" + l.getIdMacchina() + "\', "+ "\'" + l.getMessage() + "\'" +")";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3310");
        connettore.doUpdate(update);
        connettore.close();
    }
    
    @Override
    public void restoreConsistency(Log l) throws Exception {
        String delete = "DELETE FROM LOG WHERE timestamp = " + "\'" + 
                l.getTimestamp() + "\' AND idMacchina = " + "\'" + 
                l.getIdMacchina() + "\' AND message = "+ "\'" + 
                l.getMessage() + "\'";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3310");
        connettore.doUpdate(delete);
        connettore.close();
        System.out.println("Restore consistency of Replica 5");
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
        serialize("replica5queue.dat");
        if(num.getTimestamp() < vn.getTimestamp()) num.setTimestamp(vn.getTimestamp());
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
                    } catch (RuntimeException ex) {
                        System.out.println("Problem in storing content. " 
                            + this.toString() + " down");
                        return false;
                    }
                }
                else break;
            }
        }
        serialize("replica5queue.dat");
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
                System.out.println("Found a element. Added to replica5queue");
                --y;
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File not found! Your replica5queue will be empty!");
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
        ConnettoreMySQL connettore = new ConnettoreMySQL("3310");
        if(connettore.testConnection(1)) {
            System.out.println(this.toString() + " sending HeartBeat");

            faultDetector.receive("fifth");
        }
        connettore.close();
    }
    
    @Override
    public boolean pingAckResponse() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3310");
        if(connettore.testConnection(3)) {
            connettore.close();
            return true;
        }
        return false;
    }
}
