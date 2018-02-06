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
public class VersionNumber implements Serializable {
    
    private Integer timestamp;
    private Integer id;
    
    public VersionNumber(Integer timestamp, Integer id) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public VersionNumber() {}

    public Integer getId() {
        return id;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
