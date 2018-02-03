/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Log;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        try {
            proxy.writeResult(log);
        } catch (SQLException ex) {
            Logger.getLogger(InsertBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
}
