package rdf.parser.shell;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;



public class Database {

    private static Log log;


    private Connection connection;

    private Properties properties;


    public Connection openConnection() {
        log.info("Establishing source (relational) connection. URL = " + properties.getProperty("db.url"));
        if (connection == null) {
            try {
                String driver = properties.getProperty("db.driver");
                Class.forName(driver);
                String dbConnectionString = properties.getProperty("db.url");
                if (dbConnectionString == null) {
                    log.error("The property db.url cannot be empty! It must contain a valid database connection string.");
                    //System.exit(1);
                }
                connection = DriverManager.getConnection(dbConnectionString, properties.getProperty("db.login"), properties.getProperty("db.password"));

                log.info("Established source (relational) connection.");
                return connection;
            } catch (Exception e) {
                log.error(e.toString());
                log.error("Error establishing source (relational) connection! Please check your connection settings.");
                //System.exit(1);
            }
        } else {
            return connection;
        }
        return null;
    }



    public  ResultSet  query(String query) {
        ResultSet result = null;

        try {
            if (connection == null) openConnection();

            //PreparedStatement preparedStatement = connection.prepareStatement(query);
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            result = statement.executeQuery(query);
        } catch (SQLException e) {
            log.error("Error executing query! Query was: " + query + " \n "+ e.toString());



            //System.exit(1);
        }
        return result;
    }

    /**
     *
     * Create a PreparedStatement with the query string and get its metadata. If this works, the query string is ok (but nothing is executed)
     *
     */
    public void testQuery(String query) {
        try {
            if (connection == null) openConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(query);

            ResultSetMetaData m = preparedStatement.getMetaData();
            log.info ("Query is ok. Retrieves a dataset with " + m.getColumnCount() + " column(s).");

            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Error testing query! Query was: " + query);
            //System.exit(1);
        }
    }





    public void setLog(Log log) {
        this.log = log;
    }



    public void setProperties(Properties properties) {
        this.properties = properties;
    }


}