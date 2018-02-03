/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 *
 * @author zartyuk
 */
public class VersionNumber {
    
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
}
