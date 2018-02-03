/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author zartyuk
 */
@Stateless
public class ReadBean implements ReadBeanRemote {

    @EJB
    private ProxyLocal proxy;
    
    public String readBean() {
        return proxy.readResult();
    }
}
