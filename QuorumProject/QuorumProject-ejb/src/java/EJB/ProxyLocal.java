package EJB;

import Util.Log;
import javax.ejb.Local;

/**
 * Interfaccia locale del bean Proxy
 * @author zartyuk
 */

@Local
public interface ProxyLocal {
    
    public String readResult();

    public boolean writeResult(Log l);

    public void removeReplica(ReplicaBeanLocal b);
    
}
