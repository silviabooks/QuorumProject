package Util;

/**
 * Contatore utilizzato dal Proxy per contare il numero di repliche ancora connesse
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
