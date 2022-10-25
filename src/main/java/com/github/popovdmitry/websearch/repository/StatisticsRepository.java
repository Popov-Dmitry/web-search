package com.github.popovdmitry.websearch.repository;

import com.github.popovdmitry.websearch.utils.ConfigUtils;
import com.github.popovdmitry.websearch.utils.Tables;

import java.sql.*;
import java.util.Map;

public class StatisticsRepository {

    private final Connection connection;

    public StatisticsRepository() throws SQLException {
        connection = DriverManager.getConnection(
                ConfigUtils.getProperty("DB.URL"),
                ConfigUtils.getProperty("DB.USERNAME"),
                ConfigUtils.getProperty("DB.PASSWORD")
        );
        init();
    }

    private void init() throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                        "(id serial constraint rows_count_pk primary key, word_list int, url_list int, " +
                        "word_location int, link_between_url int, link_word int, date date);",
                Tables.ROWS_COUNT_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                        "(id serial constraint top_n_words_pk primary key, word text, count int, date date);",
                Tables.TOP_N_WORDS_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                        "(id serial constraint top_n_domains_pk primary key, domain text, count int, date date);",
                Tables.TOP_N_DOMAINS_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                        "(id serial constraint words_count_pk primary key, pages_processed int, count int, date date);",
                Tables.WORDS_COUNT_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                        "(id serial constraint link_between_count_pk primary key, pages_processed int, count int, date date);",
                Tables.LINK_BETWEEN_URL_COUNT_TABLE));
        statement.close();
    }

    public void close() throws SQLException {
        connection.close();
    }

    public void addRowsCount(Map<String, Integer> rowsCount) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "INSERT INTO %s (word_list, url_list, word_location, link_between_url, link_word, date) " +
                        "VALUES (?, ?, ?, ?, ?, ?);",
                Tables.ROWS_COUNT_TABLE));
        preparedStatement.setInt(1, rowsCount.get(Tables.WORD_LIST_TABLE));
        preparedStatement.setInt(2, rowsCount.get(Tables.URL_LIST_TABLE));
        preparedStatement.setInt(3, rowsCount.get(Tables.WORD_LOCATION_TABLE));
        preparedStatement.setInt(4, rowsCount.get(Tables.LINK_BETWEEN_URL_TABLE));
        preparedStatement.setInt(5, rowsCount.get(Tables.LINK_WORD_TABLE));
        preparedStatement.setDate(6, new Date(new java.util.Date().getTime()));
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public void addRowsCount(Integer pagesProcessedCount, Integer rowsCount, String table) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "INSERT INTO %s (pages_processed, count, date) VALUES (?, ?, ?);",
                table));
        preparedStatement.setInt(1, pagesProcessedCount);
        preparedStatement.setInt(2, rowsCount);
        preparedStatement.setDate(3, new Date(new java.util.Date().getTime()));
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public void addTopN(Map<String, Integer> topNMap, String table) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "INSERT INTO %s (%s, count, date) VALUES (?, ?, ?);",
                table,
                table.equals(Tables.TOP_N_WORDS_TABLE) ? "word" : "domain")
        );
        connection.setAutoCommit(false);

        topNMap.forEach((key, value) -> {
            try {
                preparedStatement.setString(1, key);
                preparedStatement.setInt(2, value);
                preparedStatement.setDate(3, new Date(new java.util.Date().getTime()));
                preparedStatement.addBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        connection.commit();
        connection.setAutoCommit(true);

        preparedStatement.executeBatch();
        preparedStatement.close();
    }
}
