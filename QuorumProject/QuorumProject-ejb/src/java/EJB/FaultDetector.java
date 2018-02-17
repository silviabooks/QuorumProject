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
 * Rilevatore di Fault delle repliche
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

    // TODO : Il singleton gestisce già la concorrenza dato che ogni metodo è lockato in write di default
    // Non serve una hashmap concorrente (????) *********************************
    
    /** 
     * Mappa (Repliche, Array di due elementi booleani)
     * Il primo campo dell'array booleano specifica se la replica è sospettata per questo ciclo di Fault Detection
     * (False = Sospettata, True = Non Sospettata)
     * Il secondo campo è utilizzato dalla procedura specifica di pingAck
     * Se tale campo è false, la replica è considerata caduta
     */
    
    private ConcurrentHashMap<ReplicaBeanLocal, ArrayList<Boolean>> replicas =
            new ConcurrentHashMap<ReplicaBeanLocal, ArrayList<Boolean>>();
    
    /**
     * Lista repliche sospette di Fault
     */
    
    private ArrayList<ReplicaBeanLocal> suspected = new ArrayList<>();
    
    @PostConstruct
    private void init() {
        replicas.put(this.firstReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.secondReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.thirdReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.fourthReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        replicas.put(this.fifthReplica, new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        System.out.println("Iniziatilize Replicas in FaultDetector");
        //System.out.println(replicas.toString());
    }
    
    /**
     * Utilizzato dalle repliche per inviare HeartBeat
     * Il metodo utilizza il patter matching per risalire alla replica che ha inviato il messaggio
     * @param s 
     */
    @Override
    public void receive(String s) {
        switch(s) {
            
            // Il controllo eseguito in ogni case permette di verificare 
            // se la replica è caduta in precedenza (Repliche statiche)
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
    
    /**
     * Utilizzato per ricercare le repliche sospette di Fault
     * L'annotazione @Schedule permette di specificare un timer che 
     * viene chiamato ogni periodo di tempo specificato
     */
    @Schedule(second="3/3", minute = "*", hour = "*", persistent = false)
    private void verifyReplicas() {
        for(Map.Entry<ReplicaBeanLocal, ArrayList<Boolean>> entry : replicas.entrySet()) {
            
            //Controlla se la replica non ha mandato HeartBeat nell'ultimo ciclo di Fault Detection
            if(entry.getValue().get(0).booleanValue() == false && entry.getValue().get(1).booleanValue() == true) {
                suspected.add(entry.getKey());
                System.out.println("Added suspected replica " + entry.getKey().toString());
            }
            
            //Resetta il ciclo di Fault Detection
            if(entry.getValue().get(1).booleanValue() == true) {
                entry.setValue(new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
            }
        }
        pingAck();
    }
    
    
    /**
     * Avvia un controllo specifico per verificare se la replica è realmente in Fault
     */
    private void pingAck() {
        for(int i =0; i<suspected.size(); i++) {
            if(!suspected.get(i).pingAckResponse()) {
                //La replica non risponde. Essa viene considerata in Fault
                System.out.println("Connection lost! Remove replica from proxy list");
                
                //Il FaultDetector avvisa il proxy che quella replica non 
                //deve essere contattata durante letture e scritture
                proxy.removeReplica(suspected.get(i));
                
                //La replica viene etichettata come Replica in Fault
                replicas.get(suspected.get(i)).set(1, Boolean.FALSE);
            }
        }
        if(!suspected.isEmpty()) {
            suspected.clear();
            System.out.println("There are some replicas in suspected array. Cleared suspected array!");
        }
    }
}
