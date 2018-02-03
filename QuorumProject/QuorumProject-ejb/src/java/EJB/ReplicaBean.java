/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EJB;

import Connettore.ConnettoreMySQL;
import Util.Log;
import Util.VersionNumber;
import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.ejb.Stateless;

/**
 *
 * @author zartyuk
 */
@Stateless(name = "firstReplica")
public class ReplicaBean implements ReplicaBeanLocal {
    
    private VersionNumber num = new VersionNumber(0,1);
    
    @Override
    public VersionNumber getNum() {
        return num;
    }
    
    @Override
    public String readReplica() throws SQLException {
        ArrayList<Log> logs = new ArrayList<>();
        String query = "SELECT * FROM LOG";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        ResultSet rs = connettore.doQuery(query);
        
        while(rs.next()){
            logs.add(new Log(rs.getTimestamp("timestamp"), rs.getString("idMacchina"),rs.getString("message")));
        }
        connettore.close();
        return new Gson().toJson(logs);
    }
    
    @Override
    public void writeReplica(Log l) throws SQLException {
        System.out.println(l.getTimestamp().toString());
        String update = "INSERT INTO LOG VALUES(" + "\'" + l.getTimestamp() + "\'," + "\'" + l.getIdMacchina() + "\', "+ "\'" + l.getMessage() + "\'" +")";
        ConnettoreMySQL connettore = new ConnettoreMySQL("3306");
        connettore.doUpdate(update);
        connettore.close();
    }
    
    @Override
    public void test() {
        System.out.println("Sono la prima replica");
    }
    
    @Override
    public void updateTimestamp(int timestamp) {
        num.setTimestamp(timestamp);
    }
}
