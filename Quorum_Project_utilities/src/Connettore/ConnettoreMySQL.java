/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Connettore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zartyuk
 */
public class ConnettoreMySQL {
    
     private Connection connection;

    public ConnettoreMySQL(String port) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:" + port + "/ELENCO", "root", "root");
            
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("CONNESSIONE FALLITA " + port);
        }
    }
    
    public boolean testConnection(int i) {
         try {
            if(connection!=null) return connection.isValid(i);
         } catch (SQLException ex) {
             Logger.getLogger(ConnettoreMySQL.class.getName()).log(Level.SEVERE, null, ex);
         }
         return false;
    }
    
    public ResultSet doQuery(String query) throws SQLException{
        Statement stm = connection.createStatement();
        
        return stm.executeQuery(query);
    }
    
    public boolean doUpdate(String update) {
        Statement stm = null;
        try {
            stm = connection.createStatement();
            stm.executeUpdate(update);
            return true;
        } catch (SQLException ex) {
            System.out.println("Write failed: " + update);
        } finally {
            if(stm != null) try {
                stm.close();
            } catch (SQLException ex) {
                Logger.getLogger(ConnettoreMySQL.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return false;
    }
    
    public void close() {
         try {
             if(connection != null && !connection.isClosed()) connection.close();
         } catch (SQLException ex) {
             Logger.getLogger(ConnettoreMySQL.class.getName()).log(Level.SEVERE, null, ex);
         }
    }
}
