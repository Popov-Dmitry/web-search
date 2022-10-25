package com.github.popovdmitry.websearch.repository;

import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.sql.*;

public class Repository {
    private final Connection connection;

    public Repository() throws SQLException {
        this.connection = DriverManager.getConnection(
                ConfigUtils.getProperty("DB.URL"),
                ConfigUtils.getProperty("DB.USERNAME"),
                ConfigUtils.getProperty("DB.PASSWORD")
        );
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        connection.close();
    }

    public ResultSet selectAllFrom(String table) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT * FROM %s;",
                table
        ));
        return preparedStatement.executeQuery();
    }

    public Integer selectRowsCountFrom(String table) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT count(*) FROM %s;",
                table
        ));
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getInt(1);
    }

    public ResultSet selectFromWhere(String table, String name, String value) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE %s = ?;",
                table,
                name
        ));
        preparedStatement.setString(1, value);
        return preparedStatement.executeQuery();
    }

    public ResultSet selectFromWhere(String table, String name, Integer value) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE %s = ?;",
                table,
                name
        ));
        preparedStatement.setInt(1, value);
        return preparedStatement.executeQuery();
    }

    public boolean isExist(String table, String name, String value) throws SQLException {
        return selectFromWhere(table, name, value).next();
    }
}
