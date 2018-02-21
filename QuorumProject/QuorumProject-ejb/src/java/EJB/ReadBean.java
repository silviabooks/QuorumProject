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
     * @return same return value of readWithQuery from Proxy EJB
     */
    public String readBean() {
        return proxy.readWithQuery("SELECT * FROM LOG;");
    }
    
    /**
     * Reads all entries of a specified machine ID
     * @param idMacchina
     * @return same return value of readWithQuery from Proxy EJB
     */
    @Override
    public String readMachineIDBean(String idMacchina) {
        return proxy.readWithQuery("SELECT * FROM LOG WHERE idMacchina='" 
                + idMacchina + "';");
    }

    /**
     * Reads last log inserted in the DB
     * @return same return value of readWithQuery from Proxy EJB
     */
    @Override
    public String readLastLogBean() {
        return proxy.readWithQuery("SELECT * FROM LOG "
                + "ORDER BY timestamp DESC LIMIT 1;");
    }

    /**
     * Reads logs with timestamps in a specific interval
     * @param begin timestamp of the beginning of the interval
     * @param end timestamp of the end of the interval
     * @return same return value of readWithQuery from Proxy EJB
     */
    @Override
    public String readTimestampIntervalBean(String begin, String end) {
        return proxy.readWithQuery("SELECT * FROM LOG WHERE timestamp "
                + "BETWEEN '" + begin + "' and '" + end + "';");
    }
    
    
}
