/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Log;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author zartyuk
 */
@Stateless
public class InsertBean implements InsertBeanRemote {

    @EJB
    private ProxyLocal proxy;
    public boolean insertBean(Log log) {
        return proxy.writeResult(log);
    }
}
