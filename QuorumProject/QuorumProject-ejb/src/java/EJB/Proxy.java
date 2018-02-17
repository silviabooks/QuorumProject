/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Counter;
import Util.Log;
import Util.VersionNumber;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
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
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class Proxy implements ProxyLocal {

    @EJB(beanName = "secondReplica")
    private ReplicaBeanLocal secondReplica;

    @EJB(beanName = "firstReplica")
    private ReplicaBeanLocal firstReplica;
    
    @EJB(beanName = "thirdReplica")
    private ReplicaBeanLocal thirdReplica;
    
    @EJB(beanName = "fourthReplica")
    private ReplicaBeanLocal fourthReplica;
    
    @EJB(beanName = "fifthReplica")
    private ReplicaBeanLocal fifthReplica;
     
    private List<ReplicaBeanLocal> replicas = Collections.synchronizedList(new ArrayList<>());
       
    /**
     * Set the Quorum value for 5 replicas
     * Read = 2
     * Write = 4
     */
    private int quorumRead = 2;
    private int quorumWrite = 4;
    
    @PostConstruct
    private void init() {
        firstReplica.init();
        secondReplica.init();
        thirdReplica.init();
        fourthReplica.init();
        fifthReplica.init();
        replicas.add(this.firstReplica);
        replicas.add(this.secondReplica);
        replicas.add(this.thirdReplica);
        replicas.add(this.fourthReplica);
        replicas.add(this.fifthReplica);
        System.out.println("Iniziatilize Replicas in Proxy");
    }
    
    @Lock(LockType.READ)
    @Override
    public String readResult() {
        if(replicas.size() < quorumRead) {
            System.out.println("I can't perform a read cause there are no sufficient replicas");
            return null;
        }
        /**
         * Verificare che la auxReplica serva realmente
         * Implementare la read similmente per quanto fatto in write
         * Gestire meglio il try catch in write per la RuntimeException
         * Che succede se la lettura viene fatta mentre una replica cade? Gestiscila
         * 
         */
        
        List<ReplicaBeanLocal> auxReplica = new ArrayList<>();
        for(int i=0; i<replicas.size(); i++)
            auxReplica.add(replicas.get(i));
        Collections.shuffle(auxReplica);
        ReplicaBeanLocal aux = auxReplica.get(0);
        for (int i=1; i<quorumRead; i++) {
            if (auxReplica.get(i).getNum().getTimestamp() > aux.getNum().getTimestamp()) aux = auxReplica.get(i);
            else if(auxReplica.get(i).getNum().getTimestamp() == aux.getNum().getTimestamp()) {
                if(auxReplica.get(i).getNum().getId() > aux.getNum().getId()) aux = auxReplica.get(i);
            }
        }
        return aux.readReplica(); //If a replica fault response, retry?
    }
    
    @Lock(LockType.WRITE)
    @Override
    public boolean writeResult(Log l) {
        // if two or more replicas are down, the quorum is not reached
        if(replicas.size() < quorumWrite) {
            System.out.println("I can't perform a write cause there are no sufficient replicas");
            return false;
        }
        VersionNumber num = new VersionNumber(-1,0);
        Counter counter = new Counter();
        
        for (int i = 0; i < replicas.size(); i++) {
            if (replicas.get(i).getNum().getTimestamp() > num.getTimestamp()) num = replicas.get(i).getNum();
            else if(replicas.get(i).getNum().getTimestamp() == num.getTimestamp()) {
                if(replicas.get(i).getNum().getId() > num.getId()) num = replicas.get(i).getNum();
            }
            replicas.get(i).writeReplica(l);
        }
        for (int i=0; i < replicas.size(); i++)
            replicas.get(i).updateVersionNumber(num, l);
        
        boolean[] verifyQuorum = new boolean[replicas.size()];
        
        for (int i=0; i<replicas.size(); i++)
            verifyQuorum[i] = replicas.get(i).commit();
        for (int i=0; i<replicas.size(); i++) {
            if(verifyQuorum[i] == true) 
                counter.increment();
        }       
        if(counter.getValue() < quorumWrite) {
            for(int j = 0; j<replicas.size(); j++) {
                try {
                    replicas.get(j).restoreConsistency(l);
                } catch (NullPointerException ex) {
                    System.out.println("Impossible restore consistency of a fallen replica");
                }
            }
        }
        counter.reset();
        return true;
    }
    
    @Override
    public void removeReplica(ReplicaBeanLocal b) {
        replicas.remove(b);
        System.out.println(replicas.toString());
    }
}
