/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 *
 * @author zartyuk
 */
public class Log implements Serializable {
    private Timestamp timestamp;
    private String idMacchina;
    private String message;

    public Log(Timestamp timestamp, String idMacchina, String message) {
        this.timestamp = timestamp;
        this.idMacchina = idMacchina;
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getIdMacchina() {
        return idMacchina;
    }

    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return "Log{" + "timestamp=" + timestamp + ", idMacchina=" + idMacchina + ", message=" + message + '}';
    }
}
