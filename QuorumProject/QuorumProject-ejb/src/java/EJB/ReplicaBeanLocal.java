/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Util.Log;
import Util.VersionNumber;
import java.io.IOException;
import java.sql.SQLException;
import javax.ejb.Local;

/**
 *
 * @author zartyuk
 */
@Local
public interface ReplicaBeanLocal {

    public String readReplica() throws SQLException;

    public void writeReplica(Log l) throws SQLException;

    public void test();

    public VersionNumber getNum();

    public void updateVersionNumber(int timestamp, Log l);

    public boolean pingAckResponse() throws SQLException;
}
