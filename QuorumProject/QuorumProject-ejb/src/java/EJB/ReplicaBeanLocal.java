/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Log;
import Util.VersionNumber;
import java.sql.SQLException;
import javax.ejb.Local;

/**
 *
 * @author zartyuk
 */
@Local
public interface ReplicaBeanLocal {

    public String readReplica() throws SQLException;

    public void writeReplica(Log l);

    public VersionNumber getNum();

    public void updateVersionNumber(VersionNumber num, Log l);

    public boolean pingAckResponse();

    public void init();

    public void restoreConsistency(Log l);

    public boolean commit();
}
