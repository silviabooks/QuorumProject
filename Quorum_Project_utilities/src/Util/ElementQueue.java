/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.io.Serializable;

/**
 *
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
