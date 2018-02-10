/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
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
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class FaultDetector implements FaultDetectorLocal {
    
    @EJB
    private ProxyLocal proxy;

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

    private ConcurrentHashMap<ReplicaBeanLocal, ArrayList<Boolean>> replicas =
            new ConcurrentHashMap<ReplicaBeanLocal, ArrayList<Boolean>>();
    
    private ArrayList<ReplicaBeanLocal> suspected = new ArrayList<>();
    
    @PostConstruct
    private void init() {
        replicas.put(this.firstReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.secondReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.thirdReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.fourthReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.fifthReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        System.out.println("Iniziatilize Replicas in FaultDetector");
        System.out.println(replicas.toString());
    }
    
    public void receive(String s) {
        switch(s) {
            case "first": if(replicas.get(firstReplica).get(1).booleanValue() == true) {
                    replicas.get(firstReplica).set(0, Boolean.TRUE);
                }
                break;
            case "second": if(replicas.get(secondReplica).get(1).booleanValue() == true) {
                    replicas.get(secondReplica).set(0, Boolean.TRUE);
                }
                break;
            case "third": if(replicas.get(thirdReplica).get(1).booleanValue() == true) {
                    replicas.get(thirdReplica).set(0, Boolean.TRUE);
                }
                break;
            case "fourth": if(replicas.get(fourthReplica).get(1).booleanValue() == true) {
                    replicas.get(fourthReplica).set(0, Boolean.TRUE);
                }
                break;
            case "fifth": if(replicas.get(fifthReplica).get(1).booleanValue() == true) {
                    replicas.get(fifthReplica).set(0, Boolean.TRUE);
                }
                break;
        }
    }
    
    @Schedule(second="12/12", minute = "*", hour = "*", persistent = false)
    private void verifyReplicas() {
        for(Map.Entry<ReplicaBeanLocal, ArrayList<Boolean>> entry : replicas.entrySet()) {
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
            if(!suspected.get(i).pingAckResponse()) {
                System.out.println("Connection lost! Remove replica from proxy list");
                proxy.removeReplica(suspected.get(i));
                replicas.get(suspected.get(i)).set(1, Boolean.FALSE);
            }
        }
        if(!suspected.isEmpty()) {
            suspected.clear();
            System.out.println("There are some replicas in suspected array. Cleared suspected array!");
        }
    }
}
