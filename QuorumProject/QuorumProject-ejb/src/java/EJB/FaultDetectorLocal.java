package EJB;

import javax.ejb.Local;

/**
 * Interfaccia locale del bean che si occupa della Fault Detection
 * @author zartyuk
 */
@Local
public interface FaultDetectorLocal {

    public void receive(String s);
    
}
