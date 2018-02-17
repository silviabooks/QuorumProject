package EJB;

import Util.Log;
import javax.ejb.Remote;

/**
 * Interfaccia remota del bean che si occupa dell'inserimento dei Log
 * @author zartyuk
 */

@Remote
public interface InsertBeanRemote {

    public boolean insertBean(Log log);
    
}
