/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    //private boolean[] response = new boolean[2]; //Syncronize me
    
    private ConcurrentHashMap<ReplicaBeanLocal, ArrayList<Boolean>> myMap =
            new ConcurrentHashMap<ReplicaBeanLocal, ArrayList<Boolean>>();

    //private ArrayList<ReplicaBeanLocal> replicas = new ArrayList<>();
    
    private ArrayList<ReplicaBeanLocal> suspected = new ArrayList<>();
    
    @PostConstruct
    private void init() {
        myMap.put(this.firstReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        myMap.put(this.secondReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        System.out.println(myMap.toString());
        //replicas.add(this.firstReplica);
        //replicas.add(this.secondReplica);
        System.out.println("Iniziatilize array of Replicas in FaultDetector");
    }
    
    public void receive(String s) {
        switch(s) {
            case "first": if(myMap.get(firstReplica).get(1).booleanValue() == true) {
                    myMap.get(firstReplica).set(0, Boolean.TRUE);
                }
                break;
            case "second": if(myMap.get(secondReplica).get(1).booleanValue() == true) {
                    myMap.get(secondReplica).set(0, Boolean.TRUE);
                }
                break;
            case "third": break;
            case "fourth": break;
            case "fifth": break;
        }
        
        //response[i] = true;
    }
    
    @Schedule(second="12/12", minute = "*", hour = "*", persistent = false)
    private void verifyReplicas() {
        /*for (int i = 0; i<2; i++) {
            if (response[i] == false) {
                suspected.add(replicas.get(i));
                System.out.println("Added suspected replica " + replicas.get(i).toString());
            }
        }
        for (int i = 0; i<2; i++) {
            response[i] = false;
        }*/
        for(Map.Entry<ReplicaBeanLocal, ArrayList<Boolean>> entry : myMap.entrySet()) {
            if(entry.getValue().get(0).booleanValue() == false && entry.getValue().get(1).booleanValue() == true) {
                suspected.add(entry.getKey());
                System.out.println("Added suspected replica " + entry.getKey().toString());
            }
            if(entry.getValue().get(1).booleanValue() == true) {
                entry.setValue(new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
            }
        }
        pingAck();
    }
    
    private void pingAck() {
        for(int i =0; i<suspected.size(); i++) {
            try {
                if(!suspected.get(i).pingAckResponse()) {
                    System.out.println("Connection lost! Remove replica from proxy list");
                    proxy.removeReplica(suspected.get(i));
                    myMap.get(suspected.get(i)).set(1, Boolean.FALSE);
                    //replicas.remove(suspected.get(i));
                }
            } catch (SQLException ex) {
                Logger.getLogger(FaultDetector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(!suspected.isEmpty()) {
            suspected.clear();
            System.out.println("Cleared suspected array");
        }
    }
}
