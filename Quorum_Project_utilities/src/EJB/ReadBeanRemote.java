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
    
    /**
     * Reads last log in the DB
     * @return 
     */
    public String readLastLogBean();
    
    /**
     * Reads logs with timestamps in a specific interval
     * @param begin timestamp of the beginning of the interval
     * @param end timestamp of the end of the interval
     * @return 
     */
    public String readTimestampIntervalBean(String begin, String end);
}
