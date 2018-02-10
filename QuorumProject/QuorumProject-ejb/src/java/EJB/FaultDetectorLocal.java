/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import javax.ejb.Local;

/**
 *
 * @author zartyuk
 */
@Local
public interface FaultDetectorLocal {

    public void receive(String s);
    
}
