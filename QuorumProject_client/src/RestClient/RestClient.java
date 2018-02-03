/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RestClient;

import Util.Log;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.util.Date;

/**
 *
 * @author zartyuk
 */
public class RestClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Client client = Client.create();
        Date date = new Date();
        Log log = new Log(new java.sql.Timestamp(date.getTime()) ,"prova", "spero funzioni");
        System.out.println(log.toString());
        String string = new Gson().toJson(log);
        WebResource webResourcePost = client.resource("http://localhost:8080/QuorumProject-war/gestione/log/post");
        ClientResponse rispostaPost = webResourcePost.post(ClientResponse.class, string);
        System.out.println(rispostaPost.getEntity(String.class));
        
        WebResource webResource = client.resource("http://localhost:8080/QuorumProject-war/gestione/log/get");
        ClientResponse rispostaGet = webResource.get(ClientResponse.class);
        System.out.println(rispostaGet.getEntity(String.class));
    }
    
}
