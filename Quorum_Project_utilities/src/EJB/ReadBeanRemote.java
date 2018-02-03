/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import javax.ejb.Remote;

/**
 *
 * @author zartyuk
 */
@Remote
public interface ReadBeanRemote {

    public String readBean();
    
}
