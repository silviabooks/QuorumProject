package EJB;

import javax.ejb.Remote;

/**
 * Interfaccia remota del bean che si occupa della lettura dei Log
 * @author zartyuk
 */
@Remote
public interface ReadBeanRemote {

    public String readBean();
    
    // TODO add method declarations
    public String readMachineIDBean(String idMacchina);
}
