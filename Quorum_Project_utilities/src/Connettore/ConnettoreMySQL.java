package Connettore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe di supporto per connettersi al database
 * @author zartyuk
 */

public class ConnettoreMySQL {
    
    private Connection connection;

    /**
     * Crea un connettore verso una specifica replica. 
     * Le porte concesse variano nel range 3306-3310
     * @param port 
     */
    public ConnettoreMySQL(String port) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:"
                    + port + "/ELENCO", "root", "root");
            
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("CONNESSIONE FALLITA " + port);
        }
    }
    
    /**
     * Effettua un test di connessione per verificare se essa è ancora attiva
     * Il parametro passato specifica il timeout del test di connessione
     * @param i
     * @return 
     */
    public boolean testConnection(int i) {
         try {
            if(connection!=null) return connection.isValid(i);
         } catch (SQLException ex) {
             Logger.getLogger(ConnettoreMySQL.class.getName())
                     .log(Level.SEVERE, null, ex);
         }
         return false;
    }
    
    /**
     * Esegue una query Select
     * @param query
     * @return
     * @throws SQLException
     * @throws NullPointerException 
     */
    
    public ResultSet doQuery(String query) 
            throws SQLException, NullPointerException {
        Statement stm = connection.createStatement();
        return stm.executeQuery(query);
    }
    
    /**
     * Esegue una query Delete o Insert
     * @param update
     * @return 
     */
    public boolean doUpdate(String update) {
        Statement stm = null;
        try {
            stm = connection.createStatement();
            stm.executeUpdate(update);
            return true;
        } catch (SQLException ex) {
            System.out.println("Update failed: " + update);
        } finally {
            if(stm != null) try {
                stm.close();
            } catch (SQLException ex) {
                Logger.getLogger(ConnettoreMySQL.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        return false;
    }
    
    /**
     * Closes DB connection (if active)
     */
    public void close() {
        try {
            if(connection != null && !connection.isClosed()) 
                connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(ConnettoreMySQL.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
}
