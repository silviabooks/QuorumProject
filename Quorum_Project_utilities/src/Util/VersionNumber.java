package Util;

import java.io.Serializable;

/**
 * VersionNumber dei ReplicaManager. Si aggiorna ad ogni transazione eseguita
 * Utilizzata anche nella coda dei ReplicaManager per garantire Total Ordering
 * @author zartyuk
 */
public class VersionNumber implements Serializable {
    
    private int timestamp;
    private int id;
    
    public VersionNumber(int timestamp, int id) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public VersionNumber() {}

    public int getId() {
        return id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public void setId(int id) {
        this.id = id;
    }
}
