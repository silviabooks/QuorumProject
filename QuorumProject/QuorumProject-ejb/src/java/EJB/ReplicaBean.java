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
 * ReplicaManager della replica con porta 3306
 * @author zartyuk
 */

@Stateless(name = "firstReplica")
public class ReplicaBean implements ReplicaBeanLocal {

    @EJB
    private FaultDetectorLocal faultDetector;
    
    //Version Number della replica
    private static VersionNumber num;
    
    //Coda in cui sono contenuti i Log prima della fase di commit
    private static List<ElementQueue> queue = 
            Collections.synchronizedList(new LinkedList<>());
    
    /**
     * Inizializza il ReplicaManager
     */
    
    @Override
    public void init() {
        this.num = new VersionNumber(0,1);
        System.out.println(this.toString() + " trying to find an existing queue");
        
        //Carica elementi precedentemente salvati su file in coda
        unserialize("replica1queue.dat");
    }
    
    /**
     * Getter
     * @return Version number of the replica
     */
    @Override
    public VersionNumber getNum() {
        return this.num;
    }
    
    /**
     * Legge tutti i Log presenti nel database
     * @return stringa in formato Json se l'operazione và a buon fine
     */
    @Override
    public String readReplica() {
        try {
            ArrayList<Log> logs = new ArrayList<>();
            String query = "SELECT * FROM LOG";
            ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
            ResultSet rs = connettore.doQuery(query);
            
            while(rs.next()){
                logs.add(new Log(rs.getTimestamp("timestamp"),
                        rs.getString("idMacchina"),
                        rs.getString("message")));
            }
            connettore.close();
            return new Gson().toJson(logs);
        } catch (SQLException ex) {
            Logger.getLogger(ReplicaBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            System.out.println("Replica 1 was selected, but is probably faultly. Retry the read!");
        }
        return "Selected a fault replica. Retry!";
    }
    
    // TODO add methods with different queries
    
    /**
     * Scrive il Log in coda e la riordina
     * @param l 
     */
    @Override
    public void writeReplica(Log l) {
        //Incrementa il Version Number della replica per la nuova transazione
        num.setTimestamp(num.getTimestamp() + 1);
        
        //Aggiunge un nuovo elemento in coda
        queue.add(new ElementQueue(new VersionNumber(num.getTimestamp(), num.getId()), l, false));
        Collections.sort(queue, new ElementQueueComparator());
        
        /*Iterator<ElementQueue> iter = queue.iterator();
        while (iter.hasNext()) {
            ElementQueue e = iter.next();
            System.out.print("Element in replica1queue: " + 
                    e.getLog().toString() + " " + 
                    e.isConfirmed() + " " + 
                    e.getNum().getTimestamp());
        }*/
    }
    
    /**
     * Esegue l'Insert del Log nella replica
     * @param l 
     */
    private void updateDatabase(Log l) {
        String update = "INSERT INTO LOG VALUES(" + "\'" + l.getTimestamp() + "\'," 
                + "\'" + l.getIdMacchina() + "\', "+ "\'" + l.getMessage() + "\'" +")";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        connettore.doUpdate(update);
        connettore.close();
    }
    
    /**
     * Esegue la Delete del Log che è stato inserito nella replica
     * Viene usato per garantire il corretto raggiungimento del quorum
     * @param l
     * @throws NullPointerException 
     */
    @Override
    public void restoreConsistency(Log l) throws NullPointerException {
        String delete = "DELETE FROM LOG WHERE timestamp = " + "\'" + 
                l.getTimestamp() + "\' AND idMacchina = "+ "\'" + 
                l.getIdMacchina() + "\' AND message = "+ "\'" + 
                l.getMessage() + "\'";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        connettore.doUpdate(delete);
        connettore.close();
        System.out.println("Restore consistency of Replica 1");
    }
    
    /**
     * Aggiorna il Version Number dell'elemento in coda
     * Tale Version number è stato deciso dal proxy per garantire il Total Ordering
     * @param vn
     * @param l 
     */
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
        
        //Riordina la coda
        Collections.sort(queue, new ElementQueueComparator());
        
        //Scrive la coda su file
        serialize("replica1queue.dat");
        
        //Aggiorna il version number della replica per allinearlo alle altre
        if(num.getTimestamp() < vn.getTimestamp()) num.setTimestamp(vn.getTimestamp());
    }
    
    /**
     * Scrive i valori in coda sulla replica
     * Salva la coda modificata su file
     * @return true se la procedura è andata a buon fine, false altrimenti
     */
    @Override
    public boolean commit() {
        
        //Controlla se la testa della coda è stata confermata dall'updateVersionNumber
        if(!queue.isEmpty() && queue.get(0).isConfirmed() == true) {
            
            //Itera tutti gli elementi in coda a partire dalla testa
            //Se sono confermati, viene fatta l'update nella replica
            Iterator<ElementQueue> ite = queue.iterator();
            while (ite.hasNext()) {
                ElementQueue e = ite.next();
                if(e.isConfirmed()) {
                    try {
                        updateDatabase(e.getLog());
                        ite.remove();
                    } catch (RuntimeException ex) {
                        System.out.println("Problem in storing content. " + 
                            this.toString() + " down");
                        return false;
                    }
                }
                else break;
            }
        }
        
        //Riscrive la coda modificata su file
        serialize("replica1queue.dat");
        return true;
    }
    
    /**
     * Scrive sul file specificato dal parametro string
     * @param string 
     */
    private void serialize(String string) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(string));
            
            //Scrive la size della coda all'inizio. In seguito, scrive i vari elementi
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
    
    /**
     * Riempie la coda basandosi sul file specificato dal parametro string
     * @param string 
     */
    private void unserialize(String string) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(string));
            int y = ois.readInt();
            while(y!=0) {
                queue.add((ElementQueue) ois.readObject());
                System.out.println("Found a element. Added to replica1queue");
                --y;
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File not found! Your replica1queue will be empty!");
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
    
    /**
     * Invia un HeartBeat al Fault Detector
     * L'annotazione @Schedule fornisce informazioni sul periodo di invio degli HeartBeat
     */
    @Schedule(second="1/1", minute = "*", hour = "*", persistent = false)
    private void sendHeartBeat() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        
        //Verifica se la connessione è attiva
        if(connettore.testConnection(1)) {
            System.out.println(this.toString() + " sending HeartBeat");
            faultDetector.receive("first");
        }
        connettore.close();
    }
    
    /**
     * Metodo invocato dal Fault Detector per verificare se la replica è realmente in Fault
     * Testa la connessione per un periodo di tempo più lungo
     * @return true se la connessione è ancora valida, false altrimenti
     */
    @Override
    public boolean pingAckResponse() {
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        if(connettore.testConnection(3)) {
            connettore.close();
            return true;
        }
        return false;
    }
}
