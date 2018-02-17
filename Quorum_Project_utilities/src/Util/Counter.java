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
public class Counter {
    int value;
        
    public Counter() {
        this.value = 0;
    }

    public int getValue() {
        return value;
    }

    public void reset() {
        this.value = 0;
    }
        
    public void increment() {
        this.value = this.value + 1;
    }
}
