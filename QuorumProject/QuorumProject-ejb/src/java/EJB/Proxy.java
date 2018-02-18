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
 * Coordinatore dei ReplicaManager. Permette al lato Front End di vedere le repliche come unico database
 * Accesso ai metodi sincronizzato: è permesso l'accesso a un solo scrittore o a più lettori
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
    
    //Lista delle repliche che partecipano al quorum
    private List<ReplicaBeanLocal> replicas = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Set del quorum per 5 repliche
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
        //System.out.println(replicas.toString());
    }
    
       
    
    /**
     * Metodo Lockato in Read: più lettori possono accedere concorrentemente
     * Si occupa di fornire i dati richiesti
     * @return null se il quorum non è raggiungibile
     */
    @Lock(LockType.READ)
    @Override
    public String readResult() {
        //Verifica se è possibile raggiungere il quorum
        if(replicas.size() < quorumRead) {
            System.out.println("I can't perform a read cause there are no sufficient replicas");
            return null;
        }
        
        //Inizializza una Lista ausiliaria
        List<ReplicaBeanLocal> auxReplica = new ArrayList<>();
        for(int i=0; i<replicas.size(); i++)
            auxReplica.add(replicas.get(i));
        //Esegue lo shuffle per selezionare due replice in maniera casuale
        Collections.shuffle(auxReplica);
        ReplicaBeanLocal aux = auxReplica.get(0);
        //Confronta i timestamp delle repliche e legge da quella più aggiornata
        for (int i=1; i<quorumRead; i++) {
            if (auxReplica.get(i).getNum().getTimestamp() > aux.getNum().getTimestamp()) 
                aux = auxReplica.get(i);
            else if(auxReplica.get(i).getNum().getTimestamp() == aux.getNum().getTimestamp()) {
                if(auxReplica.get(i).getNum().getId() > aux.getNum().getId()) 
                    aux = auxReplica.get(i);
            }
        }
        return aux.readReplica(); //If a replica fault response, retry? //************************
    }
    
    /**
     * 
     * @param q stringa con la query da passare al RM
     * @return 
     */
    @Lock(LockType.READ)
    @Override
    public String readWithQuery(String q) {
        //Verifica se è possibile raggiungere il quorum
        if(replicas.size() < quorumRead) {
            System.out.println("I can't perform a read cause there are no sufficient replicas");
            return null;
        }
        //Inizializza una Lista ausiliaria
        List<ReplicaBeanLocal> auxReplica = new ArrayList<>();
        for(int i=0; i<replicas.size(); i++)
            auxReplica.add(replicas.get(i));
        //Esegue lo shuffle per selezionare due replice in maniera casuale
        Collections.shuffle(auxReplica);
        ReplicaBeanLocal aux = auxReplica.get(0);
        //Confronta i timestamp delle repliche e legge da quella più aggiornata
        for (int i=1; i<quorumRead; i++) {
            if (auxReplica.get(i).getNum().getTimestamp() > aux.getNum().getTimestamp()) 
                aux = auxReplica.get(i);
            else if(auxReplica.get(i).getNum().getTimestamp() == aux.getNum().getTimestamp()) {
                if(auxReplica.get(i).getNum().getId() > aux.getNum().getId()) 
                    aux = auxReplica.get(i);
            }
        }
        // TODO cambiare con la funzione che accetta la query come parametro
        return aux.queryReadReplica(q);

    }
    
    /**
     * Metodo Lockato in Write: un solo scrittore può accedere
     * Si occupa di scrivere i Log forniti
     * @return false se il quorum non è raggiungibile
     */
    @Lock(LockType.WRITE)
    @Override
    public boolean writeResult(Log l) {
        //Verifica se è possibile raggiungere il quorum
        if(replicas.size() < quorumWrite) {
            System.out.println("I can't perform a write cause there are no sufficient replicas");
            return false;
        }
        
        //Ottiene dalle repliche disponibili la proposta del version number 
        VersionNumber num = new VersionNumber(-1,0);
        
        for (int i = 0; i < replicas.size(); i++) {
            if (replicas.get(i).getNum().getTimestamp() > num.getTimestamp()) num = replicas.get(i).getNum();
            else if(replicas.get(i).getNum().getTimestamp() == num.getTimestamp()) {
                if(replicas.get(i).getNum().getId() > num.getId()) num = replicas.get(i).getNum();
            }
            //Invia alla replica il consenso per scrivere nella sua coda
            replicas.get(i).writeReplica(l);
        }
        
        //Eseue l'update del version number basandosi sulle proposte fatte dalle repliche
        for (int i=0; i < replicas.size(); i++)
            replicas.get(i).updateVersionNumber(num, l);
        
        //Inizializza un contatore e un vettore di boolean per la fase di commit
        Counter counter = new Counter();
        boolean[] verifyQuorum = new boolean[replicas.size()];
        
        for (int i=0; i<replicas.size(); i++)
            //Esegue il commit. Se il commit fallisce, l'elemento corrispondente del vettore booleano è settato a false
            verifyQuorum[i] = replicas.get(i).commit();
        
        //Conta il numero di repliche il cui commit è andato a buon fine
        for (int i=0; i<replicas.size(); i++) {
            if(verifyQuorum[i] == true) 
                counter.increment();
        }
        
        //Se il counter non ha raggiunto il quorum avvia la procedura di restore
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
    
    /**
     * Metodo Lockato in Write: solo uno scrittore può accedervi
     * Viene invocato dal Fault Detector per rimuovere la replica dalla lista delle repliche disponibili
     * @param b 
     */
    @Lock(LockType.WRITE)
    @Override
    public void removeReplica(ReplicaBeanLocal b) {
        replicas.remove(b);
        System.out.println(replicas.toString());
    }

    
}
