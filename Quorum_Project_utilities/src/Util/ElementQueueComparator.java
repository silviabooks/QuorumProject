/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import java.util.Comparator;

/**
 *
 * @author zartyuk
 */
public class ElementQueueComparator implements Comparator<ElementQueue> {

    public ElementQueueComparator() {
    }
    
    @Override
    public int compare(ElementQueue o1, ElementQueue o2) {
        int timestampCmp = o1.getNum().getTimestamp().compareTo(o2.getNum().getTimestamp());
        if(timestampCmp != 0) return timestampCmp;
        return o1.getNum().getId().compareTo(o2.getNum().getId());
    }
    
}
