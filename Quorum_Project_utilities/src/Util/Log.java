package Util;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Log da salvare all'interno delle repliche
 * @author zartyuk
 */

public class Log implements Serializable {
    private Timestamp timestamp;
    private String idMacchina;
    private String message;
    
    /**
     * Empty constructor
     */
    public Log() {}
    
    /**
     * Contructor with all params
     * @param timestamp
     * @param idMacchina
     * @param message 
     */
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

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setIdMacchina(String idMacchina) {
        this.idMacchina = idMacchina;
    }

    public void setMessage(String message) {
        this.message = message;
    }
        
    @Override
    public String toString() {
        return "Log {" + "timestamp = " + timestamp + ", "
                + "idMacchina = " + idMacchina + ", message = " + message + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Log other = (Log) obj;
        if (!Objects.equals(this.idMacchina, other.idMacchina)) {
            return false;
        }
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        return true;
    }
}
