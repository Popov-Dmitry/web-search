package com.github.popovdmitry.websearch.repository;

import com.github.popovdmitry.websearch.utils.ConfigUtils;

import java.sql.*;

public class Repository {

    private final String WORD_LIST_TABLE = ConfigUtils.getProperty("WORD_LIST_TABLE");
    private final String URL_LIST_TABLE = ConfigUtils.getProperty("URL_LIST_TABLE");
    private final String WORD_LOCATION_TABLE = ConfigUtils.getProperty("WORD_LOCATION_TABLE");
    private final String LINK_BETWEEN_URL_TABLE = ConfigUtils.getProperty("LINK_BETWEEN_URL_TABLE");
    private final String LINK_WORD_TABLE = ConfigUtils.getProperty("LINK_WORD_TABLE");

    private final Connection connection;

    public Repository() throws SQLException {
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
                "(row_id serial constraint wordlist_pk primary key, word text, isFiltered int);",
                WORD_LIST_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial constraint urllist_pk primary key, url text);", URL_LIST_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial constraint wordlocation_pk primary key, " +
                "word_id integer constraint wordlocation_wordlist_rowid_fk references word_list, " +
                "url_id integer constraint wordlocation_urllist_rowid_fk references url_list," +
                "location integer);",
                WORD_LOCATION_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial not null constraint linkbetweenurl_pk primary key, " +
                "from_url_id integer constraint linkbetweenurl_urllist_rowid_fk references url_list, " +
                "to_url_id integer constraint linkbetweenurl_urllist_rowid_fk_2 references url_list);",
                LINK_BETWEEN_URL_TABLE));
        statement.close();

        statement = connection.createStatement();
        statement.execute(String.format(
                "create table if not exists %s " +
                "(row_id serial not null constraint linkword_pk primary key, " +
                "word_id integer constraint linkword_wordlist_rowid_fk references word_list," +
                "link_id integer constraint linkword_linkbetweenurl_rowid_fk references link_between_url);",
                LINK_WORD_TABLE));
        statement.close();
    }

    public void close() throws SQLException {
        connection.close();
    }

    public Integer addWord(String word, Integer isFiltered) throws SQLException {
        ResultSet resultSet = selectFromWhere(WORD_LIST_TABLE, "word", word);
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        
        PreparedStatement preparedStatement =
                connection.prepareStatement(String.format(
                        "INSERT INTO %s (word, is_filtered) VALUES (?, ?)",
                        WORD_LIST_TABLE), Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, word);
        preparedStatement.setInt(2, isFiltered);
        preparedStatement.executeUpdate();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        generatedKeys.next();
        Integer wordId = generatedKeys.getInt(1);
        preparedStatement.close();
        return wordId;
    }

    public void addWordLocation(Integer wordId, Integer urlId, Integer location) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE (word_id = ? AND url_id = ? AND location = ?)",
                WORD_LOCATION_TABLE
        ));
        preparedStatement.setInt(1, wordId);
        preparedStatement.setInt(2, urlId);
        preparedStatement.setInt(3, location);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return;
        }

        preparedStatement =
                connection.prepareStatement(String.format(
                        "INSERT INTO %s (word_id, url_id, location) VALUES (?, ?, ?)",
                        WORD_LOCATION_TABLE));
        preparedStatement.setInt(1, wordId);
        preparedStatement.setInt(2, urlId);
        preparedStatement.setInt(3, location);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public Integer addUrl(String url) throws SQLException {
        ResultSet resultSet = selectFromWhere(URL_LIST_TABLE, "url", url);
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        PreparedStatement preparedStatement =
                connection.prepareStatement(String.format(
                        "INSERT INTO %s (url) VALUES (?)",
                        URL_LIST_TABLE), Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, url);
        preparedStatement.executeUpdate();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        generatedKeys.next();
        Integer urlId = generatedKeys.getInt(1);
        preparedStatement.close();
        return urlId;
    }

    public Integer addLinkBetweenUrl(Integer fromUrlId, Integer toUrlId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE (from_url_id = ? AND to_url_id = ?)",
                LINK_BETWEEN_URL_TABLE
        ));
        preparedStatement.setInt(1, fromUrlId);
        preparedStatement.setInt(2, toUrlId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        preparedStatement =
                connection.prepareStatement(String.format(
                        "INSERT INTO %s (from_url_id, to_url_id) VALUES (?, ?)",
                        LINK_BETWEEN_URL_TABLE), Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setInt(1, fromUrlId);
        preparedStatement.setInt(2, toUrlId);
        preparedStatement.executeUpdate();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        generatedKeys.next();
        Integer linkBetweenUrlId = generatedKeys.getInt(1);
        preparedStatement.close();
        return linkBetweenUrlId;
    }

    public void addLinkText(String[] linkWords, Integer linkBetweenUrlId) throws SQLException {
        PreparedStatement preparedStatement =
                connection.prepareStatement(String.format(
                        "INSERT INTO %s (word_id, link_id) VALUES (?, ?)",
                        LINK_WORD_TABLE));
        connection.setAutoCommit(false);

        for (String word : linkWords) {
            ResultSet resultSet = selectFromWhere(WORD_LIST_TABLE, "word", word);
            resultSet.next();
            int wordId = resultSet.getInt(1);
            preparedStatement.setInt(1, wordId);
            preparedStatement.setInt(2, linkBetweenUrlId);
            preparedStatement.addBatch();

        }
        connection.commit();
        connection.setAutoCommit(true);

        preparedStatement.executeBatch();
        preparedStatement.close();
    }

    public ResultSet selectFromWhere(String table, String name, String value) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE %s = ?",
                table,
                name
        ));
        preparedStatement.setString(1, value);
        return preparedStatement.executeQuery();
    }

    public boolean isExist(String table, String name, String value) throws SQLException {
        return selectFromWhere(table, name, value).next();
    }
}
