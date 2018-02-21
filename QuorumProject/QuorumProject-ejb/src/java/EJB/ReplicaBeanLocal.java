package EJB;

import Util.Log;
import Util.VersionNumber;
import javax.ejb.Local;

/**
 * Interfaccia locale dei ReplicaManager
 * @author zartyuk
 */

@Local
public interface ReplicaBeanLocal {
    
    public String queryReadReplica(String q);

    public void writeReplica(Log l);

    public VersionNumber getNum();

    public void updateVersionNumber(VersionNumber num, Log l);

    public boolean pingAckResponse();

    public void init();

    public void restoreConsistency(Log l) throws Exception;

    public boolean commit();
}
