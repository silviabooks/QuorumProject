/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author zartyuk
 */
@Path("/log")
public class RestAPI {
    
    private static final String BEAN_ENDPOINT = "java:global/QuorumProject/QuorumProject-ejb/";
    
    @GET
    @Path("/get")
    @Produces(MediaType.TEXT_PLAIN)
    public String showLog() {
        ReadBeanRemote readBean = lookupReadBeanRemote();
        String aux = readBean.readBean();
        if(aux!=null) return aux;
        else return("Problem in read. Can't reach read quorum!");
    }
    
    @POST
    @Path("/post")
    @Consumes(MediaType.TEXT_PLAIN)
    public String addLog(String l) {
        Log log = new Gson().fromJson(l, Log.class);
        InsertBeanRemote insertBean = lookupInsertBeanRemote();
        
        if (insertBean.insertBean(log)) return "LOG AGGIUNTO";
        else return "Problem in write. Can't reach write quorum!";
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
