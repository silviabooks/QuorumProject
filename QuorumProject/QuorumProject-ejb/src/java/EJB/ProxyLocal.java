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

    public void example();

    public String readResult();

    public void writeResult(Log l) throws SQLException;
    
}
