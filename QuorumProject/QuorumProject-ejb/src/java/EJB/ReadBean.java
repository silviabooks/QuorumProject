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
    
    /**
     * Simple read
     * @return same return value of proxy.readResult() from Proxy EJB
     */
    public String readBean() {
        return proxy.readResult();
    }
    
    // TODO add methods to make the queries

    @Override
    public String readMachineIDBean(String idMacchina) {
        return "dammi tempu ca ti perciu";
    }
}
