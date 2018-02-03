/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Log;
import javax.ejb.Remote;

/**
 *
 * @author zartyuk
 */
@Remote
public interface InsertBeanRemote {

    public boolean insertBean(Log log);
    
}
