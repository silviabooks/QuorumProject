package EJB;

import javax.ejb.Remote;

/**
 * Interfaccia remota del bean che si occupa della lettura dei Log
 * @author zartyuk
 */
@Remote
public interface ReadBeanRemote {

    /**
     * Simple read
     * @return
     */
    public String readBean();
    
    /**
     * Reads all entries of a specified machine ID
     * @param idMacchina 
     * @return 
     */
    public String readMachineIDBean(String idMacchina);
}
