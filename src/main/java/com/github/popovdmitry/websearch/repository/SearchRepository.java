package com.github.popovdmitry.websearch.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SearchRepository extends Repository {

    public SearchRepository() throws SQLException {
        super();
    }

    @Override
    public ResultSet selectFromWhere(String table, String name, Integer value) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(String.format(
                "SELECT * FROM %s WHERE %s = ? LIMIT 1;",
                table,
                name
        ));
        preparedStatement.setInt(1, value);
        return preparedStatement.executeQuery();
    }
}
