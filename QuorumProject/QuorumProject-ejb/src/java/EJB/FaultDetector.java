/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author zartyuk
 */
@Singleton
@Startup
public class FaultDetector implements FaultDetectorLocal {

    @EJB
    private ProxyLocal proxy;

    @EJB(beanName = "secondReplica")
    private ReplicaBeanLocal secondReplica;

    @EJB(beanName = "firstReplica")
    private ReplicaBeanLocal firstReplica;
    
    private boolean[] response = new boolean[2]; //Syncronize me

    private ArrayList<ReplicaBeanLocal> replicas = new ArrayList<>();
    
    private ArrayList<ReplicaBeanLocal> suspected = new ArrayList<>();
    
    @PostConstruct
    private void init() {
        replicas.add(this.firstReplica);
        replicas.add(this.secondReplica);
        System.out.println("Added beans in List?");
        atSchedule();
    }
    
    public void receive(int i) {
        response[i] = true;
    }
    
    @Schedule(second="*/2", persistent = false)
    public void atSchedule() {
        for (int i = 0; i<2; i++) {
            if (response[i] == false) {
                suspected.add(replicas.get(i));
                System.out.println("Replica aggiunta");
            }
        }
        for (int i = 0; i<2; i++) {
            response[i] = false;
        }
        pingAck();
    }
    
    //@AccessTimeout(value = 5, unit = TimeUnit.SECONDS)
    private void pingAck() {
        for(int i =0; i<suspected.size(); i++) {
            try {
                if(!suspected.get(i).pingAckResponse()) {
                    System.out.println("Ho perso la connessione");
                }
            } catch (SQLException ex) {
                //Logger.getLogger(FaultDetector.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Non esiste niente");
            }
        }
        suspected.clear();
        System.out.println("Pulizia");
    }
}
