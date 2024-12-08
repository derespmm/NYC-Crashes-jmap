package edu.miamioh;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private HikariDataSource dataSource;

    public void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost/cse385_final");
        config.setUsername("root");             // CHANGE THIS TO YOUR USERNAME
        config.setPassword("Matthewd430!");     // CHANGE THIS TO YOUR PASSWORD
        config.setMaximumPoolSize(100);
        config.setMinimumIdle(5);

        dataSource = new HikariDataSource(config);
    }

    public ResultSet fetchLocations() throws SQLException {
        // Get a connection from the data source
        Connection connect = dataSource.getConnection();

        // Create a statement with scroll-insensitive and read-only properties
        Statement statement = connect.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
        );

        // Execute the query and return the result set
        return statement.executeQuery("SELECT latitude, longitude FROM collision;");
    }

    public ResultSet executeQuery(String query) throws SQLException {
        // Get a connection from the data source
        Connection connect = dataSource.getConnection();

        // Create a statement
        Statement statement = connect.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
        );

        return statement.executeQuery(query);
    }

    public void executeInsert(String query) throws SQLException {
        // Get a connection from the data source
        Connection connect = dataSource.getConnection();

        // Create a statement
        Statement statement = connect.createStatement();

        // Execute the insert query
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(connect, statement);
        }
    }

    private void close(Connection connect, Statement statement) {
        // Ensure the statement and connection are closed
        try {
            for (int i = 0; i < 3; i++) {
                if (statement != null) {
                    statement.close();
                }
                if (connect != null) {
                    connect.close();
                }
            }
        } catch (SQLException e) {
            // Log or print the exception while closing the resources
            e.printStackTrace();
        }
    }
}