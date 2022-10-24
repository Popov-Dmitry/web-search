package com.github.popovdmitry.websearch.repository;

import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.sql.*;
import java.util.Map;

public class StatisticsRepository {

    private final String WORD_LIST_TABLE = ConfigUtils.getProperty("WORD_LIST_TABLE");
    private final String URL_LIST_TABLE = ConfigUtils.getProperty("URL_LIST_TABLE");
    private final String WORD_LOCATION_TABLE = ConfigUtils.getProperty("WORD_LOCATION_TABLE");
    private final String LINK_BETWEEN_URL_TABLE = ConfigUtils.getProperty("LINK_BETWEEN_URL_TABLE");
    private final String LINK_WORD_TABLE = ConfigUtils.getProperty("LINK_WORD_TABLE");

    private final String ROWS_COUNT_TABLE = ConfigUtils.getProperty("ROWS_COUNT_TABLE");

    private final Connection connection;

    public StatisticsRepository() throws SQLException {
        connection = DriverManager.getConnection(
                ConfigUtils.getProperty("DB_URL"),
                ConfigUtils.getProperty("DB_USERNAME"),
                ConfigUtils.getProperty("DB_PASSWORD")
        );
        init();
    }

    private void init() throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                        "(id serial constraint rows_count_pk primary key, word_list int, url_list int, " +
                        "word_location int, link_between_url int, link_word int, date date);",
                ROWS_COUNT_TABLE));
        statement.close();

    }

    public void addRowsCount(Map<String, Integer> rowsCount) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "INSERT INTO %s (word_list, url_list, word_location, link_between_url, link_word, date) " +
                        "VALUES (?, ?, ?, ?, ?, ?);", ROWS_COUNT_TABLE));
        preparedStatement.setInt(1, rowsCount.get(WORD_LIST_TABLE));
        preparedStatement.setInt(2, rowsCount.get(URL_LIST_TABLE));
        preparedStatement.setInt(3, rowsCount.get(WORD_LOCATION_TABLE));
        preparedStatement.setInt(4, rowsCount.get(LINK_BETWEEN_URL_TABLE));
        preparedStatement.setInt(5, rowsCount.get(LINK_WORD_TABLE));
        preparedStatement.setDate(6, new Date(new java.util.Date().getTime()));
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }
}