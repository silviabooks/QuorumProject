package Rest;

import EJB.InsertBeanRemote;
import EJB.ReadBeanRemote;
import Util.Log;
import com.google.gson.Gson;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Rest API per interagire con il sistema
 * Esempio  
 * - Eventi  ordinati  nel  tempo  per  tutte  le  macchine
 * - Eventi  ordinati  nel  tempo  per  una  singola  macchina
 * - Eventi in una certa finestra temporale
 * - Ultimo evento 
 * @author zartyuk
 */

@Path("/log")
public class RestAPI {
    
    private static final String BEAN_ENDPOINT = "java:global/QuorumProject/QuorumProject-ejb/";
    
    /**
     * Gets all saved log
     * @return string in JSON format if the read is successful
     */
    @GET
    @Path("/get")
    @Produces(MediaType.TEXT_PLAIN)
    public String showLog() {
        ReadBeanRemote readBean = lookupReadBeanRemote();
        
        //Chiama il bean remoto per la read
        String aux = readBean.readBean();
        if(aux != null) 
            return aux;
        else 
            return("Problem in read. Can't reach read quorum!");
    }
    
    /**
     * Gets all logs from a specific machine
     * @param idMacchina
     * @return string in JSON format if the read is successful
     */
    @GET
    @Path("/get/{idMacchina}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIDLogs(@PathParam("idMacchina") String idMacchina) {
        // Remote bean call
        ReadBeanRemote readBean = lookupReadBeanRemote();
        String result = readBean.readMachineIDBean(idMacchina);
        if(result != null)
            return result;
        else 
            return("Problem in read. Can't reach read quorum!");
    }
    
    /**
     * Reads last log inserted in the DB
     * @return string in JSON format if the read is successful
     */
    @GET
    @Path("/getLast")
    @Produces(MediaType.TEXT_PLAIN)
    public String getLastLog() {
        // Remote bean call
        ReadBeanRemote readBean = lookupReadBeanRemote();
        String result = readBean.readLastLogBean();
        if(result != null)
            return result;
        else 
            return("Problem in read. Can't reach read quorum!");
    }
    
    /**
     * Reads logs with timestamps in a specific interval
     * @param begin timestamp of the beginning of the interval
     * @param end timestamp of the end of the interval
     * @return string in JSON format if the read is successful
     */
    @GET 
    @Path("/timestamp/{begin}/{end}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getTimestampInterval(@PathParam("begin") String begin, 
            @PathParam("end") String end) {
        // Remote bean call
        ReadBeanRemote readBean = lookupReadBeanRemote();
        String result = readBean.readTimestampIntervalBean(begin, end);
        if(result != null)
            return result;
        else 
            return("Problem in read. Can't reach read quorum!");
    }
    
    /**
     * Adds a log entry in the DB from a POST request from the client
     * @param l log in JSON format
     * @return string with the outcome of the insertion
     */
    @POST
    @Path("/post")
    @Consumes(MediaType.TEXT_PLAIN)
    public String addLog(String l) {
        //Conversione da JSON a oggetto Log
        Log log = new Gson().fromJson(l, Log.class);
        InsertBeanRemote insertBean = lookupInsertBeanRemote();
        
        //Chiama il bean remoto per la write
        if (insertBean.insertBean(log)) 
            return "LOG AGGIUNTO";
        else return "Problem in write. Can't reach write quorum!";
        // TODO Se necessario per la GUI, modificare le stringhe di ritorno
    }
    
    private ReadBeanRemote lookupReadBeanRemote() {
        try {
            Context c = new InitialContext();
            return (ReadBeanRemote) c.lookup(BEAN_ENDPOINT + "ReadBean!EJB.ReadBeanRemote");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    private InsertBeanRemote lookupInsertBeanRemote() {
        try {
            Context c = new InitialContext();
            return (InsertBeanRemote) c.lookup(BEAN_ENDPOINT + "InsertBean!EJB.InsertBeanRemote");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }
    
}
