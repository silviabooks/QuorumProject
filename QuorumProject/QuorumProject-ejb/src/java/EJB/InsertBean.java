package EJB;

import Util.Log;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Bean demandato alla esecuzione degli inserimenti dei Log
 * @author zartyuk
 */
@Stateless
public class InsertBean implements InsertBeanRemote {

    @EJB
    private ProxyLocal proxy;
    /**
     * Simple insertion of a log 
     * @param log to be inserted
     * @return same return value of proxy.writeResult() from Proxy EJB 
     */
    public boolean insertBean(Log log) {
        return proxy.writeResult(log);
    }
}
