package Util;

import java.io.Serializable;

/**
 * Elementi che vengono salvati nella coda interna ai 
 * ReplicaManager prima della fase di commit
 * @author zartyuk
 */
public class ElementQueue implements Serializable {
    private VersionNumber num;
    private Log log;
    private boolean confirmed;

    public ElementQueue(VersionNumber num, Log log, boolean confirmed) {
        this.num = num;
        this.log = log;
        this.confirmed = confirmed;
    }

    public VersionNumber getNum() {
        return num;
    }

    public Log getLog() {
        return log;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setNum(VersionNumber num) {
        this.num.setTimestamp(num.getTimestamp());
        this.num.setId(num.getId());
    }
}
