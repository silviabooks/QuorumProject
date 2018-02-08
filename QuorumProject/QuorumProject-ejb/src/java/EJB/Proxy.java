/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.ElementQueueComparator;
import Util.Log;
import Util.VersionNumber;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author zartyuk
 */
@Singleton
@Startup
public class Proxy implements ProxyLocal {

    @EJB
    private FaultDetectorLocal faultDetector;

    @EJB(beanName = "secondReplica")
    private ReplicaBeanLocal secondReplica;

    @EJB(beanName = "firstReplica")
    private ReplicaBeanLocal replicaBean;
    
    
    
    private ArrayList<ReplicaBeanLocal> replicas = new ArrayList<>();
    
    /**
     * Set the Quorum value for 5 replicas
     * Read = 2
     * Write = 4
     */
    private int quorumRead = 1;
    private int quorumWrite = 2;
    
    @PostConstruct
    private void init() {
        replicas.add(this.replicaBean);
        replicas.add(this.secondReplica);
        System.out.println("Iniziatilize array of Replicas in Proxy");
    }
    
    @Lock(LockType.READ)
    public String readResult() {
        Collections.shuffle(replicas);
        ReplicaBeanLocal aux = replicas.get(0);
        for (int i=1; i<quorumRead; i++) {
            if (replicas.get(i).getNum().getTimestamp() > aux.getNum().getTimestamp()) aux = replicas.get(i);
            else if(replicas.get(i).getNum().getTimestamp() == aux.getNum().getTimestamp()) {
                if(replicas.get(i).getNum().getId() > aux.getNum().getId()) aux = replicas.get(i);
            }
        }
        aux.test();
        try {
            return aux.readReplica();
        } catch (SQLException ex) {
            Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return "{}";
    }
    
    @Lock(LockType.WRITE)
    public boolean writeResult(Log l) throws SQLException {
        if(replicas.size()<quorumWrite) {
            System.out.println("I can't perform a read cause there is no sufficient replicas");
            return false;
        }
        Collections.shuffle(replicas);
        VersionNumber num = replicas.get(0).getNum();
        replicas.get(0).writeReplica(l);
        for (int i=1; i<quorumWrite; i++) {
            if (replicas.get(i).getNum().getTimestamp() > num.getTimestamp()) num = replicas.get(i).getNum();
            else if(replicas.get(i).getNum().getTimestamp() == num.getTimestamp()) {
                if(replicas.get(i).getNum().getId() > num.getId()) num = replicas.get(i).getNum();
            }
            replicas.get(i).writeReplica(l);
        }
        for (int i=0; i<quorumWrite; i++) {
            replicas.get(i).updateVersionNumber(num.getTimestamp()+1, l);
        }
        return true;
    }
    
    public void removeReplica(ReplicaBeanLocal b) {
        replicas.remove(b);
        System.out.println(replicas.toString());
    }
}
