package Util;

import java.util.Comparator;

/**
 * Comparatore di ElementQueue. Si occupa di riordinare gli elementi nella coda dei ReplicaManager
 * @author zartyuk
 */

public class ElementQueueComparator implements Comparator<ElementQueue> {

    public ElementQueueComparator() {
    }
    
    @Override
    public int compare(ElementQueue o1, ElementQueue o2) {
        int timestampCmp = o1.getNum().getTimestamp() - o2.getNum().getTimestamp();
        if(timestampCmp != 0) 
            return timestampCmp;
        return o1.getNum().getId() - o2.getNum().getId();
    }
    
}
