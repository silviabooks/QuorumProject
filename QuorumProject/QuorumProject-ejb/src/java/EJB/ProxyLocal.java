/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Log;
import java.sql.SQLException;
import javax.ejb.Local;

/**
 *
 * @author zartyuk
 */
@Local
public interface ProxyLocal {

    public String readResult();

    public boolean writeResult(Log l) throws SQLException;

    public void removeReplica(ReplicaBeanLocal b);
    
}
