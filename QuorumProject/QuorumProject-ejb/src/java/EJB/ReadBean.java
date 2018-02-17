package EJB;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Bean demandato alla esecuzione delle letture dei Log
 * @author zartyuk
 */

@Stateless
public class ReadBean implements ReadBeanRemote {

    @EJB
    private ProxyLocal proxy;
    
    public String readBean() {
        return proxy.readResult();
    }
}
